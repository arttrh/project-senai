# 🍔 Cantina SENAI

<div align="center">

**Sistema de pedidos e estoque para cantina escolar — telas em Thymeleaf e API REST no mesmo projeto**

![Java](https://img.shields.io/badge/Java-21-ED8936?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Views-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)

</div>

---

## 📋 Sobre o Projeto

A cantina da escola vende produto que acaba. Esse é o problema inteiro do
projeto: quando alguém fecha um pedido, o estoque tem que cair junto — e se
zerar, o produto some do cardápio sozinho.

Tudo acontece dentro de uma transação em `PedidoService.pedidoFeito()`: valida a
quantidade contra o estoque, adiciona o item ao pedido, dá a baixa e, se o
estoque chegar a zero, marca o produto como inativo. Ou tudo vai, ou nada vai.

O projeto tem duas caras sobre a mesma camada de serviço: **telas em Thymeleaf**
pra quem usa a cantina no dia a dia, e uma **API REST** em `/api/*` pra quem
quiser consumir de fora. É Spring MVC em camadas — controller, service,
repository — sem arquitetura hexagonal, e isso é proposital: ele é o mais antigo
dos meus projetos e serve de comparação com os que vieram depois.

### ✨ Funcionalidades

- ✅ Cadastro e gestão de **produtos**, com ativação e desativação
- ✅ Controle de **estoque** por produto
- ✅ **Pedidos** com múltiplos itens e quantidade por item
- ✅ Baixa automática no estoque ao fechar o pedido, dentro de uma transação
- ✅ Produto sem estoque sai do cardápio automaticamente
- ✅ Ciclo de vida do pedido em enum: `CRIADO` → `EM_PREPARACAO` → `FINALIZADO` / `CANCELADO`
- ✅ **Usuários** com três perfis: `USUARIO`, `FUNCIONARIO`, `ADMIN`
- ✅ Telas Thymeleaf e API REST compartilhando o mesmo service
- ✅ Handler global de exceções — o controller não vira um amontoado de try/catch
- ✅ Build automático no GitHub Actions a cada push

---

## 🏗️ Estrutura

```
project-senai/
├── src/
│   ├── main/
│   │   ├── java/br/com/cantina/senai/
│   │   │   ├── model/          # Usuario, Produto, Estoque, Pedido, QuantidadeProduto + enums
│   │   │   ├── dto/            # DTOs de cadastro, atualização, listagem e detalhamento
│   │   │   ├── repositorys/    # Spring Data JPA
│   │   │   ├── service/        # regra de negócio: Pedido, Produto, Estoque, Usuario
│   │   │   ├── controller/     # ViewController (Thymeleaf) e ApiController (REST)
│   │   │   ├── exceptions/     # exceções de domínio
│   │   │   ├── handler/        # tratamento global de erros
│   │   │   └── config/         # configuração do banco
│   │   └── resources/
│   │       ├── templates/      # home, cadastro, funcionario, finalizarPedido...
│   │       └── application.properties
│   └── database/
│       └── mySql.sql           # schema completo
├── Dockerfile
└── pom.xml
```

Cada área tem dois controllers: um `ViewController`, que devolve template, e um
`ApiController`, que devolve JSON. Os dois chamam o mesmo service, então a regra
de negócio existe uma vez só.

---

## 🚀 Como Rodar

### Pré-requisitos

- **Java 21+**
- **Maven 3.8+**
- **MySQL 8**

### 1. Clone o repositório

```bash
git clone https://github.com/arttrh/project-senai.git
cd project-senai/project-senai
```

### 2. Crie o banco

O schema completo está em `src/database/mySql.sql`:

```bash
mysql -u root -p < src/database/mySql.sql
```

> ⚠️ O script começa com `DROP DATABASE cantinasenai` — não rode em cima de um
> banco com dados que você queira manter.

### 3. Configure a conexão

Em `src/main/resources/application.properties`, aponte para o seu MySQL:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cantinasenai
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
```

### 4. Rode

```bash
./mvnw spring-boot:run
```

Aplicação em `http://localhost:8080`.

---

### 🐳 Com Docker

```bash
docker build -t cantina-senai .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/cantinasenai \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=sua_senha \
  cantina-senai
```

---

## 🔌 Endpoints

### Telas (Thymeleaf)

| Rota | Tela |
|------|------|
| `GET /home` | Cardápio |
| `GET /usuario/cadastrar` | Cadastro de usuário |
| `GET /funcionario/estoque` | Gestão de estoque |
| `GET /funcionario/pedidos/listar` | Pedidos abertos e em preparação |
| `GET /funcionario/pedido/finalizar` | Fechamento de pedido |

### API REST

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/produtos` | Lista produtos |
| `GET` | `/api/produtos/{id}` | Detalha um produto |
| `POST` | `/api/produtos` | Cadastra produto |
| `PUT` | `/api/produtos/{id}` | Atualiza produto |
| `DELETE` | `/api/produtos/{id}` | Remove produto |
| `GET` | `/api/estoque` | Lista o estoque |
| `PUT` | `/api/estoque/{id}` | Ajusta a quantidade |
| `GET` | `/api/pedidos` | Lista pedidos ativos |
| `POST` | `/api/pedidos` | Cria um pedido |
| `GET` | `/api/funcionario` | Lista funcionários |
| `POST` | `/api/funcionario/cadastrar` | Cadastra funcionário |

---

## 📊 Modelo de dados

```sql
usuario            id_usuario, nome, cpf, telefone, email, tipo_usuario
produto            id_produto, nome_produto, descricao_produto, produto_ativo
estoque            id_estoque, id_produto → produto, quantidade
pedido             id_pedido, id_usuario → usuario, data_pedido, status_pedido
quantidade_produto id_pedido → pedido, id_produto → produto, quantidade
```

`quantidade_produto` é a tabela de ligação entre pedido e produto: é ela que
permite um pedido com três coxinhas e dois sucos.

---

## 🛠️ Tecnologias

| Tecnologia | Papel no projeto |
|------------|------------------|
| Java 21 | Linguagem |
| Spring Boot 3 | Framework base |
| Spring Data JPA | Persistência |
| Spring Validation | Validação dos DTOs de entrada |
| Thymeleaf | Renderização das telas |
| MySQL 8 | Banco de dados |
| Lombok | Menos boilerplate nos models |
| Docker | Empacotamento em imagem |
| GitHub Actions | Build a cada push na master |

---

## 🐛 Problemas comuns

**`Table 'cantinasenai.x' doesn't exist`**
O schema não foi criado. Rode o `src/database/mySql.sql`.

**"Produto sem estoque" num produto que aparece na tela**
O produto é desativado quando o estoque zera, mas telas já carregadas continuam
mostrando o cardápio antigo. Recarregue a página.

**Porta 8080 ocupada**
Mude em `application.properties`:

```properties
server.port=8081
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
