package br.com.cantina.senai.integracao;

import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.security.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Regressao do bug mais silencioso do projeto.
 *
 * O tratador global antigo declarava um @ExceptionHandler sem valor recebendo
 * Exception, o que registra um tratador para TODAS as excecoes respondendo 404
 * com corpo vazio. Erro de validacao, conflito de regra e falha interna
 * chegavam ao front todos como "404 Not Found" sem mensagem nenhuma.
 *
 * Cada teste aqui fixa um status e um corpo diferentes; se alguem reintroduzir
 * um tratador amplo demais, todos menos o de 404 quebram.
 */
@Transactional
class TratamentoDeErroIntegracaoTest extends BaseIntegracao {

    @Test
    @DisplayName("recurso inexistente responde 404 com mensagem")
    void naoEncontradoResponde404() throws Exception {
        mockMvc.perform(get("/api/produtos/999999").with(user(cliente())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Recurso nao encontrado"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.caminho").value("/api/produtos/999999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("dados invalidos respondem 400 com a lista de campos")
    void validacaoResponde400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","cpf":"123","email":"nao-e-email","senha":"123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.camposInvalidos").isArray())
                .andExpect(jsonPath("$.camposInvalidos.length()")
                        .value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("regra de negocio violada responde 409, nao 404")
    void regraDeNegocioResponde409() throws Exception {
        String corpo = """
                {"nome":"Joao da Silva","cpf":"529.982.247-25",
                 "telefone":"(11) 98888-7777","email":"joao.unico@senai.br",
                 "senha":"senhaSegura1"}""";

        mockMvc.perform(post("/api/usuarios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/usuarios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.erro").value("Regra de negocio"));
    }

    @Test
    @DisplayName("JSON malformado responde 400")
    void jsonInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .with(user(cliente())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isso nao e json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Requisicao invalida"));
    }

    @Test
    @DisplayName("id de tipo errado na URL responde 400")
    void tipoErradoNaUrlResponde400() throws Exception {
        mockMvc.perform(get("/api/produtos/abc").with(user(cliente())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("acesso negado responde 403 com corpo, nao 404")
    void acessoNegadoResponde403() throws Exception {
        mockMvc.perform(get("/api/estoque").with(user(cliente())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a resposta de erro nao vaza stack trace nem classe interna")
    void erroNaoVazaDetalheInterno() throws Exception {
        String corpo = mockMvc.perform(get("/api/produtos/999999").with(user(cliente())))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(corpo)
                .doesNotContain("Exception")
                .doesNotContain("br.com.cantina.senai.service")
                .doesNotContain("at java.");
    }

    private UsuarioAutenticado cliente() {
        return new UsuarioAutenticado(criarUsuario(TipoUsuario.USUARIO));
    }
}
