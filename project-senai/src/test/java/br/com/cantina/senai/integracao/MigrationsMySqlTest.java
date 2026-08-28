package br.com.cantina.senai.integracao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aplica as migrations no MySQL de verdade.
 *
 * O resto da suite roda em H2 (modo MySQL), que aceita quase tudo mas nao e o
 * banco de producao: um CHECK, um tipo ou uma sintaxe que so o MySQL recusa
 * passaria despercebido ate o deploy. Este teste fecha essa lacuna.
 *
 * So roda quando MYSQL_TEST_URL esta definida (o CI sobe um servico MySQL e a
 * define); na maquina de quem nao tem MySQL, e simplesmente pulado.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = ".+")
class MigrationsMySqlTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configurarMySql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("MYSQL_TEST_URL"));
        registry.add("spring.datasource.username",
                () -> System.getenv().getOrDefault("MYSQL_TEST_USER", "root"));
        registry.add("spring.datasource.password",
                () -> System.getenv().getOrDefault("MYSQL_TEST_PASSWORD", ""));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("app.admin-inicial.email", () -> "");
        registry.add("app.admin-inicial.senha", () -> "");
    }

    @Test
    @DisplayName("as migrations sobem no MySQL e o Hibernate valida o mapeamento")
    void migrationsAplicamNoMySql() {
        // Chegar aqui ja significa que o Flyway rodou e que o ddl-auto=validate
        // aprovou cada entidade contra o schema criado pelas migrations.
        Integer versoes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);

        assertThat(versoes).isEqualTo(2);
    }

    @Test
    @DisplayName("as tabelas do dominio existem com as chaves esperadas")
    void tabelasCriadas() {
        for (String tabela : new String[]{"usuario", "produto", "estoque", "pedido", "item_pedido"}) {
            Integer existe = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = ?""",
                    Integer.class, tabela);
            assertThat(existe).as("tabela %s", tabela).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("o UNIQUE que impede duas linhas de estoque para o mesmo produto existe")
    void estoqueTemUniqueDeProduto() {
        Integer unico = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = DATABASE() AND table_name = 'estoque'
                  AND constraint_name = 'uk_estoque_produto'""", Integer.class);

        assertThat(unico).isEqualTo(1);
    }

    @Test
    @DisplayName("o catalogo inicial foi carregado")
    void catalogoCarregado() {
        Integer produtos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM produto", Integer.class);
        Integer estoques = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM estoque", Integer.class);

        assertThat(produtos).isEqualTo(10);
        assertThat(estoques).as("todo produto do catalogo nasce com estoque").isEqualTo(produtos);
    }
}
