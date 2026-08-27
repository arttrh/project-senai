package br.com.cantina.senai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: sobe o contexto inteiro, aplica as migrations do Flyway e deixa o
 * Hibernate validar o mapeamento contra o schema real (ddl-auto=validate).
 *
 * Qualquer divergencia entre entidade e migration quebra aqui, no CI, e nao no
 * primeiro boot em producao.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProjectSenaiApplicationTests {

	@Autowired
	private WebApplicationContext contexto;

	@Test
	@DisplayName("o contexto sobe com as migrations aplicadas e o schema validado")
	void contextLoads() {
		assertThat(contexto).isNotNull();
	}

	@Test
	@DisplayName("as camadas principais estao registradas no contexto")
	void camadasRegistradas() {
		assertThat(contexto.getBeansWithAnnotation(org.springframework.stereotype.Service.class))
				.isNotEmpty();
		assertThat(contexto.getBean(br.com.cantina.senai.service.PedidoService.class)).isNotNull();
		assertThat(contexto.getBean(org.springframework.security.web.SecurityFilterChain.class))
				.isNotNull();
	}
}
