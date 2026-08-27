package br.com.cantina.senai.integracao;

import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.observability.CorrelationIdFilter;
import br.com.cantina.senai.security.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** O projeto nao tinha observabilidade nenhuma antes desta camada. */
@Transactional
class ObservabilidadeIntegracaoTest extends BaseIntegracao {

    @Test
    @DisplayName("health e publico, para o Docker conseguir checar o container")
    void healthEPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("as probes de liveness e readiness respondem")
    void probesRespondem() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("metricas e prometheus exigem ADMIN")
    void metricasProtegidas() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/prometheus")
                        .with(user(new UsuarioAutenticado(criarUsuario(TipoUsuario.USUARIO)))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o ADMIN enxerga as metricas de negocio da cantina")
    void metricasDeNegocioExpostas() throws Exception {
        String corpo = mockMvc.perform(get("/actuator/prometheus")
                        .with(user(new UsuarioAutenticado(criarUsuario(TipoUsuario.ADMIN)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo)
                .contains("cantina_pedidos_fila")
                .contains("cantina_produtos_sem_estoque");
    }

    @Test
    @DisplayName("toda resposta volta com um correlation id")
    void correlationIdNaResposta() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().exists(CorrelationIdFilter.HEADER));
    }

    @Test
    @DisplayName("o correlation id enviado pelo cliente e preservado")
    void correlationIdDoClienteEPreservado() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.HEADER, "trace-abc-123"))
                .andExpect(header().string(CorrelationIdFilter.HEADER, "trace-abc-123"));
    }

    @Test
    @DisplayName("correlation id suspeito e trocado por um novo")
    void correlationIdSuspeitoEDescartado() throws Exception {
        // Nao deixa injetarem quebra de linha nem lixo no arquivo de log.
        String resultado = mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.HEADER, "malicioso\nFAKE LOG LINE"))
                .andReturn().getResponse().getHeader(CorrelationIdFilter.HEADER);

        assertThat(resultado).doesNotContain("FAKE LOG LINE").matches("[A-Za-z0-9-]+");
    }
}
