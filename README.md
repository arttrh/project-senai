# 🍔 Cantina SENAI

<div align="center">

**Sistema de pedidos e estoque para cantina escolar — telas em Thymeleaf e API REST no mesmo projeto**

![Java](https://img.shields.io/badge/Java-21-ED8936?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-Auth-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Views-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)

</div>

---

## 📋 Sobre o Projeto

A cantina da escola vende produto que acaba. Esse é o problema inteiro do
projeto: quando alguém fecha um pedido, o estoque tem que cair junto — e se
zerar, o produto some do cardápio sozinho.

O carrinho inteiro entra numa transação só, em `PedidoService.criar()`: valida
cada item contra o estoque, trava as linhas, dá a baixa, congela o preço do
momento da compra e grava o pedido. Ou tudo vai, ou nada vai — não existe pedido
pela metade nem estoque baixado à toa.

O projeto tem duas caras sobre a mesma camada de serviço: **telas em Thymeleaf**
pra quem usa a cantina no dia a dia, e uma **API REST** em `/api/*` pra quem
quiser consumir de fora. É Spring MVC em camadas — controller, service,
repository — sem arquitetura hexagonal, e isso é proposital: ele é o mais antigo
dos meus projetos e serve de comparação com os que vieram depois.

> 📄 O que mudou na refatoração e por quê está em **[REFATORACAO.md](REFATORACAO.md)**.

### ✨ Funcionalidades

- ✅ **Login** por e-mail e senha, com hash BCrypt e sessão
- ✅ **Autorização por perfil**: `USUARIO`, `FUNCIONARIO`, `ADMIN`
- ✅ Cadastro e gestão de **produtos**, com preço, categoria e desativação
- ✅ Controle de **estoque** por produto, com trava contra venda a descoberto
- ✅ **Pedidos** com múltiplos itens, criados numa transação única
- ✅ Preço **congelado no momento da compra** — reajuste não altera pedido antigo
- ✅ Cancelamento **devolve o estoque** e preserva o pedido no histórico
- ✅ Ciclo de vida em enum, com transições válidas declaradas:
     `CRIADO` → `EM_PREPARACAO` → `PRONTO` → `FINALIZADO`, e `CANCELADO` até o preparo
- ✅ Cliente acompanha e cancela **os próprios** pedidos; a cantina vê a fila toda
- ✅ Telas Thymeleaf e API REST compartilhando o mesmo service
- ✅ Handler global de erros com status e mensagem por tipo de falha
- ✅ **Migrations Flyway** — o schema é versionado, não gerado no boot
- ✅ **Observabilidade**: health checks, métricas de negócio e correlation id
- ✅ **103 testes** (unitários e de integração) rodando no CI a cada push

---

## 🏗️ Estrutura

```
.
├── .github/
│   ├── workflows/ci.yml     # build, MySQL, Docker e segurança
│   └── dependabot.yml       # maven + github-actions + docker
├── docker-compose.yml       # aplicação + MySQL
├── .env.example             # todas as variáveis de ambiente
└── project-senai/
    ├── src/
    │   ├── main/
    │   │   ├── java/br/com/cantina/senai/
    │   │   │   ├── model/          # Usuario, Produto, Estoque, Pedido, ItemPedido + enums
    │   │   │   ├── dto/            # por agregado: usuario/, produto/, estoque/, pedido/
    │   │   │   ├── repository/     # Spring Data JPA
    │   │   │   ├── service/        # regra de negócio
    │   │   │   ├── controller/
    │   │   │   │   ├── api/        # REST (JSON)
    │   │   │   │   └── view/       # Thymeleaf (HTML)
    │   │   │   ├── security/       # autenticação e principal
    │   │   │   ├── config/         # SecurityConfig, admin inicial
    │   │   │   ├── observability/  # métricas, health, correlation id
    │   │   │   ├── validation/     # validador de CPF
    │   │   │   ├── exception/      # exceções de domínio
    │   │   │   └── handler/        # tratamento global de erros
    │   │   └── resources/
    │   │       ├── db/migration/   # migrations Flyway
    │   │       ├── templates/      # login, home, cadastro, funcionario...
    │   │       └── application.properties
    │   └── test/                   # unitários + integração
    ├── Dockerfile
    └── pom.xml
```

Cada área tem dois controllers: um em `view/`, que devolve template, e um em
`api/`, que devolve JSON. Os dois chamam o mesmo service, então a regra de
negócio existe uma vez só.

---

## 🚀 Como Rodar

### 🐳 Com Docker (mais simples)

Sobe a aplicação e o MySQL juntos, sem instalar nada além do Docker:

```bash
cp .env.example .env      # preencha as senhas
docker compose up --build
```

Aplicação em `http://localhost:8080`.

O primeiro `ADMIN` é criado no boot a partir de `APP_ADMIN_EMAIL` e
`APP_ADMIN_SENHA` do `.env`. Não existe usuário embutido no código: senha em
arquivo versionado seria credencial pública.

---

### 💻 Localmente

**Pré-requisitos:** Java 21+, Maven 3.8+, MySQL 8.

```bash
git clone https://github.com/arttrh/project-senai.git
cd project-senai
cp .env.example .env
```

Crie o banco vazio — **o schema é aplicado sozinho** pelo Flyway no primeiro
boot, não há script para rodar à mão:

```sql
CREATE DATABASE cantinasenai;
CREATE USER 'cantina'@'%' IDENTIFIED BY 'sua_senha';
GRANT ALL PRIVILEGES ON cantinasenai.* TO 'cantina'@'%';
```

Exporte as variáveis e rode:

```bash
set -a; source .env; set +a
cd project-senai
./mvnw spring-boot:run
```

### Rodando os testes

```bash
cd project-senai
./mvnw verify            # 103 testes em H2, sem precisar de MySQL
```

O relatório de cobertura sai em `target/site/jacoco/index.html`.

---

## 🔌 Endpoints

Tudo exige login, exceto o que está marcado como público.

### Telas (Thymeleaf)

| Rota | Tela | Quem acessa |
|------|------|-------------|
| `GET /login` | Login | público |
| `GET /usuario/cadastrar` | Criar conta | público |
| `GET /home` | Cardápio | autenticado |
| `GET /pedido/finalizar` | Fechamento do pedido | autenticado |
| `GET /pedido/meus` | Acompanhar meus pedidos | autenticado |
| `GET /configuracoes` | Minha conta | autenticado |
| `GET /funcionario` | Painel da cantina | funcionário, admin |

### API REST

| Método | Rota | Descrição | Quem acessa |
|--------|------|-----------|-------------|
| `POST` | `/api/usuarios` | Cria conta | público |
| `GET` | `/api/usuarios/eu` | Meu perfil | autenticado |
| `PUT` | `/api/usuarios/eu` | Edita meu perfil | autenticado |
| `PUT` | `/api/usuarios/eu/senha` | Troca minha senha | autenticado |
| `GET` | `/api/usuarios` | Lista contas | admin |
| `PUT` | `/api/usuarios/{id}/tipo` | Muda o perfil de alguém | admin |
| `GET` | `/api/produtos` | Cardápio com preço e saldo | autenticado |
| `GET` | `/api/produtos/{id}` | Detalha um produto | autenticado |
| `POST` | `/api/produtos` | Cadastra produto | funcionário, admin |
| `PUT` | `/api/produtos/{id}` | Atualiza produto | funcionário, admin |
| `DELETE` | `/api/produtos/{id}` | Tira do cardápio | funcionário, admin |
| `POST` | `/api/pedidos` | Cria pedido com o carrinho inteiro | autenticado |
| `GET` | `/api/pedidos/meus` | Meus pedidos | autenticado |
| `GET` | `/api/pedidos/{id}` | Detalha (só se for meu) | autenticado |
| `POST` | `/api/pedidos/{id}/cancelar` | Cancela meu pedido | autenticado |
| `GET` | `/api/funcionario/pedidos` | Fila da cantina | funcionário, admin |
| `PATCH` | `/api/funcionario/pedidos/{id}/status` | Avança o pedido | funcionário, admin |
| `GET` | `/api/funcionario/estoque` | Estoque | funcionário, admin |
| `PUT` | `/api/funcionario/estoque/{id}` | Ajusta a quantidade | funcionário, admin |
| `GET` | `/api/estoque` | CRUD de estoque | funcionário, admin |

#### Criar um pedido

```http
POST /api/pedidos
Content-Type: application/json

{
  "itens": [
    { "idProduto": 1, "quantidade": 2 },
    { "idProduto": 5, "quantidade": 1 }
  ],
  "formaPagamento": "PIX",
  "observacao": "sem cebola"
}
```

O carrinho vai inteiro numa requisição só. Se faltar estoque para qualquer item,
**nada** é gravado e a resposta é `409` explicando qual produto faltou.

### Observabilidade

| Rota | Descrição | Quem acessa |
|------|-----------|-------------|
| `GET /actuator/health` | Saúde da aplicação | público |
| `GET /actuator/health/{liveness,readiness}` | Probes do orquestrador | público |
| `GET /actuator/metrics` | Métricas | admin |
| `GET /actuator/prometheus` | Métricas para scraping | admin |

Métricas de negócio expostas: `cantina_pedidos_criados`,
`cantina_pedidos_cancelados`, `cantina_pedidos_recusados_estoque`,
`cantina_pedidos_fila` e `cantina_produtos_sem_estoque`.

Toda resposta traz um `X-Correlation-Id`, que também aparece em cada linha de log
daquela requisição.

---

## 📊 Modelo de dados

```sql
usuario      id_usuario, nome, cpf, telefone, email, senha, tipo_usuario, ativo
produto      id_produto, nome_produto, descricao_produto, preco, categoria, produto_ativo
estoque      id_estoque, id_produto → produto (UNIQUE), quantidade, versao
pedido       id_pedido, id_usuario → usuario, data_pedido, status_pedido,
             forma_pagamento, observacao, valor_total
item_pedido  id_pedido → pedido, id_produto → produto, quantidade, preco_unitario
```

`item_pedido` é a tabela de ligação entre pedido e produto: é ela que permite um
pedido com três coxinhas e dois sucos. O `preco_unitario` guarda o preço do
momento da compra, para reajuste no cardápio não alterar pedido antigo.

O `estoque.versao` é o controle de concorrência, e o `UNIQUE` em `id_produto`
garante uma linha de estoque por produto.

O schema é criado pelas migrations em `src/main/resources/db/migration/`. O
Hibernate roda em `ddl-auto=validate`: ele confere o mapeamento, mas nunca altera
o banco.

---

## 🛠️ Tecnologias

| Tecnologia | Papel no projeto |
|------------|------------------|
| Java 21 | Linguagem |
| Spring Boot 4 | Framework base |
| Spring Security | Login, autorização por perfil, CSRF |
| Spring Data JPA | Persistência |
| Spring Validation | Validação dos DTOs de entrada |
| Flyway | Migrations versionadas do schema |
| Thymeleaf | Renderização das telas |
| MySQL 8 | Banco de dados |
| Lombok | Menos boilerplate nos models |
| Actuator + Micrometer | Health checks e métricas |
| Docker | Empacotamento em imagem, com jar em camadas |
| JUnit 5 + Mockito + AssertJ | Testes |
| H2 | Banco em memória usado só nos testes |
| JaCoCo | Cobertura |
| GitHub Actions | Build, testes, MySQL e Docker a cada push |
| Dependabot | Atualização de dependências |

---

## 🐛 Problemas comuns

**`Access denied for user` no boot**
As variáveis de ambiente não foram carregadas. Rode `set -a; source .env; set +a`
antes do `mvnw`, ou use o `docker compose`, que lê o `.env` sozinho.

**A aplicação sobe mas não consigo entrar**
Nenhum `ADMIN` foi criado. Defina `APP_ADMIN_EMAIL` e `APP_ADMIN_SENHA` (mínimo
8 caracteres) no `.env` e reinicie — o log avisa quando falta. Contas criadas
pela tela de cadastro entram sempre como `USUARIO`; para promover alguém a
funcionário use `PUT /api/usuarios/{id}/tipo` com uma conta de admin.

**`Schema-validation: missing table`**
As migrations não rodaram. Confirme que o banco existe e que o usuário tem
permissão para criar tabelas.

**O login não completa e volta para a tela**
Se estiver rodando sem HTTPS, `COOKIE_SECURE` precisa ser `false` no `.env` —
com `true` o navegador descarta o cookie de sessão.

**"Estoque insuficiente" num produto que aparece na tela**
O cardápio é atualizado a cada 30s; alguém pode ter levado o último. Recarregue.

**Porta 8080 ocupada**

```bash
SERVER_PORT=8081
```

---

## 👤 Autor

**Arthur Lucas**
GitHub: [@arttrh](https://github.com/arttrh)

---

<div align="center">

**Projeto desenvolvido durante a formação no SENAI**

⭐ Se este projeto te ajudou, deixa uma estrela!

</div>
