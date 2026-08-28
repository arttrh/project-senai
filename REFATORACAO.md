# Refatoração da Cantina SENAI

Documento complementar ao [README.md](README.md). O README explica **como usar**
o sistema; este aqui explica **o que mudou e por quê**.

O ponto de partida foi um mapeamento do projeto inteiro — modelo, serviços,
controllers, banco, front, build e pipeline — anotando cada defeito antes de
escrever qualquer linha. O que segue é o resultado desse levantamento, agrupado
por camada, com o problema original de cada item.

---

## Resumo

| | Antes | Depois |
|---|---|---|
| Testes | 1 (vazio) | 103 |
| Cobertura de linhas | — | ~75% |
| Autenticação | nenhuma | Spring Security + BCrypt |
| Schema do banco | `ddl-auto=update` | migrations Flyway |
| Preço do produto | não existia | `DECIMAL(10,2)` no modelo e na API |
| Tratamento de erro | 404 vazio para tudo | status e mensagem por tipo de erro |
| Observabilidade | nenhuma | Actuator, métricas de negócio, correlation id |
| CI | `mvn package` só na master | build, MySQL, Docker e segurança em todo branch |
| Dependabot | inexistente | maven + github-actions + docker |

---

## 1. Os cinco defeitos mais graves

### 1.1 Um `@ExceptionHandler` transformava todo erro em 404 vazio

```java
// handler/TradadorGlobalErrors.java — versão anterior
@ExceptionHandler                                     // <- sem valor
public ResponseEntity<UsuarioNotFoundException> tratarUsuarioNotFound(Exception e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();   // <- sem corpo
}
```

Um `@ExceptionHandler` sem valor, recebendo `Exception`, registra no Spring um
tratador para **todas** as exceções. Erro de validação, conflito de regra de
negócio, falha de banco e `NullPointerException` chegavam ao front do mesmo
jeito: `404 Not Found`, corpo vazio.

Isso deixava o sistema inteiro sem diagnóstico. Todos os outros bugs desta lista
ficavam invisíveis por causa dele.

**Agora:** cada família de exceção tem seu status e uma mensagem no corpo —
400 para validação (com a lista de campos), 409 para regra de negócio, 403 para
acesso negado, 404 só para o que realmente não existe. `TratamentoDeErroIntegracaoTest`
fixa cada um: um tratador amplo demais volta a quebrar quase todos.

### 1.2 Não havia autenticação nenhuma

Sem Spring Security no projeto, `/api/funcionario/pedidos`, `/api/estoque` e o
`DELETE /api/estoque/{id}` respondiam a qualquer pessoa que alcançasse a porta
8080.

**Agora:** login por e-mail e senha, hash BCrypt, autorização por perfil
(`USUARIO` / `FUNCIONARIO` / `ADMIN`), CSRF e cabeçalhos de resposta.

### 1.3 Qualquer usuário podia se promover a ADMIN

`DTOAtualizarUsuario` carregava `tipoUsuario`, e o service aplicava o campo:

```java
if (usuarioAtualizado.tipoUsuario() != null) {
    usuario.setTipoUsuario(usuarioAtualizado.tipoUsuario());   // vindo do cliente
}
```

Um `PUT` no próprio cadastro com `"tipoUsuario": "ADMIN"` bastava.

**Agora:** o campo saiu do DTO de auto-edição. A troca de perfil tem endpoint
próprio (`PUT /api/usuarios/{id}/tipo`), restrito a `ADMIN`, com trava contra
rebaixar o único administrador. Coberto por teste de unidade e de integração.

### 1.4 Todo pedido era gravado no nome do usuário 1

```java
// controller/pedido/PedidoApiController.java — versão anterior
@PostMapping
public DTODetalhamentoPedido criar(@RequestBody DTOCadastroPedido dados) {
    return pedidoService.criarPedido(dados, 1L);      // <- id fixo
}
```

**Agora:** o dono vem da sessão (`@AuthenticationPrincipal`).

### 1.5 O pedido era montado em várias transações

O fluxo antigo criava um pedido vazio e depois adicionava cada item numa chamada
separada (`pedidoFeito`). Um carrinho de três itens virava quatro transações
independentes: se a terceira falhasse por falta de estoque, as duas primeiras já
tinham baixado o saldo e o pedido ficava pela metade.

E o cancelamento era pior:

```java
// service/PedidoService.java — versão anterior
pedido.setStatusPedido(StatusPedido.CANCELADO);
pedidoRepository.deleteById(idPedido);        // apaga logo em seguida
```

O registro sumia, o estoque nunca voltava e o cliente perdia a compra do
histórico.

**Agora:** o carrinho inteiro entra numa transação só — ou tudo, ou nada.
O cancelamento devolve o estoque e preserva o pedido com status `CANCELADO`.

---

## 2. Modelo e banco de dados

### O produto não tinha preço

A interface toda falava em dinheiro — `R$ --` nos cards, total do carrinho,
"Forma de pagamento" — sobre um modelo sem nenhum campo de preço. O comentário
no `home.js` original dizia: *"O preço está de enfeite (R$ --) já que não existe
no banco"*.

Numa cantina, preço não é detalhe: é a regra de negócio central. `Produto` ganhou
`preco` (`DECIMAL(10,2)`) e `categoria`, e o `ItemPedido` guarda uma **cópia do
preço no momento da compra** — se a cantina reajustar o cardápio, pedidos
antigos não mudam de valor.

### Construtores que não construíam nada

```java
// model/estoque/Estoque.java — versão anterior
public Estoque(DTOCadastroEstoque dados) {
    this.produto = produto;          // atribui o campo a ele mesmo: sempre nulo
    this.quantidade = quantidade;    // idem
}

// model/pedido/Pedido.java — versão anterior
public Pedido(DTOCadastroPedido dados) {
    this.dataPedido = LocalDateTime.now();   // ignora o DTO recebido
}
```

Ambos removidos e substituídos por construtores que recebem o que precisam.

### Estoque permitia várias linhas para o mesmo produto

Sem `UNIQUE` em `id_produto`, nada impedia duas linhas de estoque para a mesma
coxinha — e aí `findByProduto_IdProduto` (que devolve `Optional`) estouraria
`NonUniqueResultException` no meio de uma venda. Agora a relação é 1:1 no banco
e o service recusa a segunda linha antes de chegar lá.

### Recursão infinita no `@Data`

`QuantidadeProduto` usava `@Data` do Lombok com associações bidirecionais para
`Pedido` e `Produto`. `equals`/`hashCode`/`toString` gerados assim entram em
recursão infinita. A entidade virou `ItemPedido`, com `equals`/`hashCode` por
identidade persistente.

### Corrida de estoque

Dois pedidos simultâneos do mesmo produto liam o mesmo saldo, ambos passavam na
checagem e ambos gravavam a subtração em cima da leitura antiga — estoque
negativo, cantina vendendo o que não tem.

Duas proteções: `@Version` na entidade e trava pessimista (`SELECT ... FOR
UPDATE`) na baixa da venda, com os itens travados em ordem de id para não formar
deadlock. `ConcorrenciaEstoqueTest` dispara 20 pedidos simultâneos para 10
unidades e confere que *vendidos + saldo restante* fecham com o saldo inicial —
o resultado é exatamente 10 aceitos e 10 recusados.

### Migrations no lugar de `ddl-auto=update`

O schema vivia em `src/database/mySql.sql`, um script que começava com
`DROP database cantinasenai;` — impossível de aplicar em qualquer ambiente já em
uso. E o `spring.jpa.hibernate.ddl-auto=update` deixava o Hibernate alterar o
banco sozinho no boot, sem histórico e sem rollback.

Agora são migrations Flyway versionadas, com `ddl-auto=validate`: o Hibernate
apenas confere se o mapeamento bate com o schema. Qualquer divergência quebra no
CI, não no primeiro boot em produção.

O script antigo também tinha `status_pedido VARCHAR(20) NOT NULL DEFAULT
'PENDENTE'`, um valor que não existe no enum `StatusPedido`.

---

## 3. Serviços

**`ProdutoService.atualizarNomeProduto`** tinha um `set` solto depois da guarda
de nulo:

```java
if (dados.nomeProduto() != null) {
    produto.setNomeProduto(dados.nomeProduto());
}
// ...
produto.setNomeProduto(dados.nomeProduto());   // <- anula a guarda acima
```

Um `PUT` só de preço apagava o nome do produto. Coberto por
`ProdutoServiceTest.atualizacaoParcialPreservaNome`.

**Exclusões viraram desativações.** `usuarioRepository.delete()` e
`produtoRepository.deleteById()` quebrariam a chave estrangeira de pedidos
antigos — erro 500 na cara do usuário. Agora ambos usam flag `ativo`, o
histórico é preservado e o item some do cardápio.

**Transições de status** ficam declaradas no enum `StatusPedido`, num lugar só,
em vez de espalhadas pelos controllers. Não dá para pular de `CRIADO` direto a
`FINALIZADO`.

**Checagem de dono:** o cliente só enxerga e cancela os próprios pedidos; trocar
o id na URL não dá acesso ao pedido de outra pessoa.

**Duplicidade** de e-mail, CPF e nome de produto é conferida no service, com
mensagem clara, em vez de virar `DataIntegrityViolationException` e 500.

**Transações:** trocado `jakarta.transaction.Transactional` pelo do Spring, com
`readOnly = true` nas leituras.

---

## 4. Controllers

| Antes | Agora |
|---|---|
| `UsuarioApiController` — classe vazia, sem `@RestController`, enquanto o `Cadastro.js` já chamava `/api/usuarios/cadastrar` (404) | API de contas completa |
| `ProdutoController` — classe vazia | removida |
| `POST /api/produtos` chamado `criarPedido`, não usava o corpo, não gravava nada e devolvia **201 Created** | cadastro de produto de verdade |
| `FuncionarioApiController` devolvia `List<Pedido>` — a entidade JPA — expondo o modelo interno e quebrando na serialização das associações lazy | devolve DTO |
| Sem mapeamento para `/` (404 na raiz) e sem `/configuracoes` | ambos existem |

---

## 5. Front-end

O layout foi mantido; o que estava quebrado era a ligação com o back.

- **`funcionario.js` chamava cinco funções inexistentes.** `abrirModalEstoque`,
  `salvarEstoque`, `abrirModalProduto`, `salvarProduto` e `fecharModal` estavam
  no `onclick` do HTML mas não existiam em lugar nenhum: todo clique nos botões
  de estoque era um `ReferenceError`. Também chamava
  `PATCH /api/funcionario/pedidos/{id}/status`, endpoint que nunca existiu, e
  lia `p.id`, `p.status` e `p.valorTotal`, campos que a entidade não tinha (os
  nomes reais eram `idPedido` e `statusPedido`).
- **`configuracaoUsuario.js` não existia.** O template referenciava o arquivo e o
  servidor respondia 404.
- **`cadastro.html` postava para `/home`**, rota que não aceita POST: 405.
- **`configuracaoUsuario.html` lia `usuario.matricula`**, campo que a entidade
  `Usuario` nunca teve — `SpelEvaluationException` derrubava a página inteira.
- **Caminhos do disco do autor** versionados nos templates
  (`/project-senai-dev/project-senai/src/main/resources/...`).
- **`finalizarPedido.js` mandava só o primeiro item** do carrinho
  (`JSON.stringify(itens[0])`), num formato que o back não esperava.

Além das correções: telas de **login** e de **acompanhamento de pedido** (que
faltavam), preço real em todas as telas, CSRF em toda escrita, escape de HTML no
que vem do banco, botão de confirmar travado contra duplo clique, e regras
responsivas para as telas menores.

---

## 6. Observabilidade

Não havia nenhuma. Foram adicionados:

- **Actuator** com health, info, metrics e prometheus. `health` e as probes de
  liveness/readiness ficam públicas (o Docker precisa consultá-las); o resto
  exige `ADMIN`.
- **Health check de domínio:** uma cantina sem item vendável está de pé mas não
  está funcionando — reporta `DOWN` nesse caso.
- **Métricas de negócio:** `cantina.pedidos.criados`, `cantina.pedidos.cancelados`,
  `cantina.pedidos.recusados.estoque`, `cantina.pedidos.fila` e
  `cantina.produtos.sem.estoque`.
- **Correlation id** por requisição, no MDC e em toda linha de log daquela
  chamada, devolvido no header `X-Correlation-Id`. O id vindo do cliente é
  validado antes de entrar no log, para ninguém injetar quebra de linha.

---

## 7. Testes

103 testes: unitários com Mockito nos serviços e de integração subindo o
contexto inteiro contra H2 **com as mesmas migrations do Flyway que rodam em
produção**.

| Suíte | O que fixa |
|---|---|
| `PedidoServiceTest` | criação atômica, congelamento de preço, agrupamento de linhas repetidas, transições de status, checagem de dono |
| `UsuarioServiceTest` | hash da senha, máscara de CPF, duplicidade, e a regressão da escalação de privilégio |
| `ProdutoServiceTest` | atualização parcial que não apaga o nome, desativação em vez de exclusão |
| `EstoqueServiceTest` | linha única por produto, baixa e reposição simétricas |
| `SegurancaIntegracaoTest` | endpoints fechados, CSRF, senha e CPF não vazam, redirect para o login |
| `PedidoFluxoIntegracaoTest` | compra de ponta a ponta, cancelamento devolvendo estoque |
| `TratamentoDeErroIntegracaoTest` | cada status de erro, para o 404-para-tudo não voltar |
| `ConcorrenciaEstoqueTest` | 20 pedidos simultâneos sem venda a descoberto |
| `MigrationsMySqlTest` | migrations aplicadas no MySQL de verdade (roda no CI) |

### Um defeito de configuração descoberto no caminho

`src/test/resources/application.properties` tinha o **mesmo nome** do arquivo
principal, e por isso **substituía o `application.properties` de `main` inteiro**
no classpath de teste. Nenhuma configuração real — actuator, cookie de sessão,
Flyway, logs — era exercitada pelos testes, e um erro nessas linhas só apareceria
em produção.

Virou `application-test.properties` com `@ActiveProfiles("test")`, contendo
apenas as diferenças de ambiente.

---

## 8. Build, CI e deploy

### Spring Boot 4

O projeto já apontava para o Spring Boot 4.0.5, que moveu para módulos próprios
autoconfigurações que antes vinham em `spring-boot-autoconfigure`. Sem os
módulos, **as coisas falham em silêncio**: o `flyway-core` no classpath sem
`spring-boot-starter-flyway` não roda migration nenhuma, e o
`micrometer-registry-prometheus` sem `spring-boot-starter-micrometer-metrics`
não registra o endpoint. Ambos adicionados.

### Dependabot

O `.github/dependabot.yml` **não existia**. Sem o arquivo, o Dependabot só abre
alerta de segurança — e o formulário "Create config file" do GitHub já vem
preenchido com um template de **npm**. É daí que vinha a configuração aparecer
como NPM num projeto Java.

Detalhe que faria a configuração falhar em silêncio: o `pom.xml` não está na raiz
do repositório, e sim em `/project-senai`. Um `directory: "/"` não encontraria
manifesto nenhum e o Dependabot nunca abriria PR, parecendo configurado.

Ecossistemas cobertos: **maven** (`/project-senai`), **github-actions** (`/`) e
**docker** (`/project-senai`), com as atualizações do Spring agrupadas — subir um
módulo sem os outros da mesma versão quebra o build.

### CI

O workflow anterior rodava `mvn package` só em push/PR para `master`. O novo roda
em todos os branches, com quatro jobs:

1. **Build e testes** — compila, testa, publica cobertura.
2. **Migrations no MySQL** — sobe um MySQL de serviço e aplica as migrations
   nele. A suíte roda em H2, que aceita sintaxe que o MySQL recusaria; este job
   fecha a lacuna.
3. **Imagem Docker** — constrói e **sobe o container** junto com um MySQL,
   passando só quando `/actuator/health` responde `UP`. Build que compila não
   prova que a aplicação sobe.
4. **Segurança** — procura segredo versionado e garante que o `.env` não entrou
   no repositório.

### Docker

- `-DskipTest` (sem s) é uma propriedade que o Maven ignora — corrigido para
  `-DskipTests`.
- **Jar em camadas:** 68 MB de dependências estáveis em `lib/` e 156 KB de código
  da aplicação. Um deploy comum reenvia só a segunda camada.
- Passou a rodar como **usuário sem privilégios** (era root), ganhou
  `HEALTHCHECK` e `exec` no entrypoint, para o java receber o `SIGTERM` e
  desligar graciosamente.
- `.dockerignore` — o contexto do build levava `target/` inteiro.
- `docker-compose.yml` que espera o banco ficar saudável antes de subir a
  aplicação; sem isso o Flyway tenta migrar um MySQL ainda inicializando.

### Variáveis de ambiente

`.env.example` lista todas as variáveis que a aplicação lê, comentadas. O `.env`
de verdade fica **fora do versionamento**, porque guarda senhas — o CI falha se
alguém o commitar.

Foi preciso um `.gitignore` **na raiz**: o existente fica em `project-senai/` e
vale só para aquele subdiretório, então um `.env` criado na raiz (onde o
`docker-compose` o procura) seria versionado com as senhas dentro.

O primeiro `ADMIN` é criado no boot a partir de `APP_ADMIN_EMAIL` e
`APP_ADMIN_SENHA`, e não por uma migration: senha em arquivo versionado é
credencial pública. Só o hash BCrypt chega ao banco.

---

## 9. Injeção de dependência

Já era majoritariamente por construtor, o que está correto. Os ajustes:

- Removidos os `@Autowired` redundantes em construtor único — o Spring já injeta
  sem eles.
- Serviços passaram a depender de outros **serviços** onde havia acoplamento
  direto a repositórios de outro agregado (`EstoqueService` usa `ProdutoService`,
  não `ProdutoRepository`), para a regra de "produto existe?" viver num lugar só.
- Nenhum campo `@Autowired`: todos os serviços são instanciáveis num teste
  unitário sem subir contexto do Spring — é o que `PedidoServiceTest` faz.

---

## 10. O que ficou de fora

Coisas que a interface sugere mas que o back ainda não implementa, sinalizadas
como tal em vez de fingirem funcionar:

- **Upload de foto de perfil** — o botão faz pré-visualização local; não há
  armazenamento de arquivo no servidor.
- **Preferências do usuário** (notificações, histórico, sugestões) — os toggles
  não são persistidos.
- **Pagamento real** — a forma de pagamento é registrada no pedido, mas não há
  integração com nenhum meio de pagamento.
- **Saldo SENAI** — aparece como forma de pagamento, mas não existe carteira nem
  débito de saldo.

Nenhum desses bloqueia o fluxo principal: cadastrar, logar, montar pedido, a
cantina preparar e entregar.
