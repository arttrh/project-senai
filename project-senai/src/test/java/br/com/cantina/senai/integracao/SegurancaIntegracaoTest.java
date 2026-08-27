package br.com.cantina.senai.integracao;

import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Autorizacao de ponta a ponta.
 *
 * Antes destes testes nao havia autenticacao no projeto: todos os cenarios
 * abaixo respondiam 200 para qualquer pessoa na rede.
 */
@Transactional
class SegurancaIntegracaoTest extends BaseIntegracao {

    @Test
    @DisplayName("visitante nao lista pedidos da cantina")
    void visitanteNaoAcessaPainel() throws Exception {
        mockMvc.perform(get("/api/funcionario/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("chamada de API sem sessao responde 401 em JSON, nao redirect")
    void apiRespondeJsonQuandoNaoAutenticado() throws Exception {
        // Um 302 para a tela de login faria o fetch do front receber HTML.
        mockMvc.perform(get("/api/pedidos/meus"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("cliente comum nao entra no painel do funcionario")
    void clienteNaoAcessaPainel() throws Exception {
        mockMvc.perform(get("/api/funcionario/pedidos")
                        .with(user(autenticado(TipoUsuario.USUARIO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("cliente comum nao mexe no estoque")
    void clienteNaoMexeNoEstoque() throws Exception {
        mockMvc.perform(get("/api/estoque")
                        .with(user(autenticado(TipoUsuario.USUARIO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("cliente comum nao apaga uma linha de estoque")
    void clienteNaoApagaEstoque() throws Exception {
        // Este DELETE ficava aberto na internet.
        mockMvc.perform(delete("/api/estoque/1")
                        .with(user(autenticado(TipoUsuario.USUARIO))).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("funcionario acessa o painel")
    void funcionarioAcessaPainel() throws Exception {
        mockMvc.perform(get("/api/funcionario/pedidos")
                        .with(user(autenticado(TipoUsuario.FUNCIONARIO))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("cliente nao promove ninguem a ADMIN")
    void clienteNaoPromove() throws Exception {
        Usuario alvo = criarUsuario(TipoUsuario.USUARIO);

        mockMvc.perform(put("/api/usuarios/" + alvo.getIdUsuario() + "/tipo")
                        .with(user(autenticado(TipoUsuario.USUARIO))).with(csrf())
                        .contentType("application/json")
                        .content(json(Map.of("tipoUsuario", "ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("editar o proprio perfil nao promove a conta")
    void edicaoDePerfilNaoPromove() throws Exception {
        // Regressao da escalacao de privilegio: mesmo mandando tipoUsuario no
        // corpo, o campo nao existe mais no DTO e e simplesmente ignorado.
        Usuario cliente = criarUsuario(TipoUsuario.USUARIO);

        mockMvc.perform(put("/api/usuarios/eu")
                        .with(user(new br.com.cantina.senai.security.UsuarioAutenticado(cliente)))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"nome":"Novo Nome","tipoUsuario":"ADMIN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoUsuario").value("USUARIO"));

        assertThat(usuarioRepository.findById(cliente.getIdUsuario()).orElseThrow().getTipoUsuario())
                .isEqualTo(TipoUsuario.USUARIO);
    }

    @Test
    @DisplayName("ADMIN promove um usuario a funcionario")
    void adminPromove() throws Exception {
        Usuario alvo = criarUsuario(TipoUsuario.USUARIO);

        mockMvc.perform(put("/api/usuarios/" + alvo.getIdUsuario() + "/tipo")
                        .with(user(autenticado(TipoUsuario.ADMIN))).with(csrf())
                        .contentType("application/json")
                        .content(json(Map.of("tipoUsuario", "FUNCIONARIO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoUsuario").value("FUNCIONARIO"));
    }

    @Test
    @DisplayName("escrita sem token CSRF e recusada")
    void escritaSemCsrfRecusada() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .with(user(autenticado(TipoUsuario.USUARIO)))
                        .contentType("application/json")
                        .content("""
                                {"itens":[],"formaPagamento":"PIX"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a senha nunca sai numa resposta da API")
    void senhaNuncaVazaNaResposta() throws Exception {
        Usuario cliente = criarUsuario(TipoUsuario.USUARIO);

        String corpo = mockMvc.perform(get("/api/usuarios/eu")
                        .with(user(new br.com.cantina.senai.security.UsuarioAutenticado(cliente))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain("senha").doesNotContain("$2a$");
    }

    @Test
    @DisplayName("o CPF sai mascarado")
    void cpfSaiMascarado() throws Exception {
        Usuario cliente = criarUsuario(TipoUsuario.USUARIO);

        mockMvc.perform(get("/api/usuarios/eu")
                        .with(user(new br.com.cantina.senai.security.UsuarioAutenticado(cliente))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(org.hamcrest.Matchers.startsWith("***.")));
    }

    @Test
    @DisplayName("cadastro e login sao publicos")
    void telasPublicas() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/usuario/cadastrar")).andExpect(status().isOk());
    }

    private br.com.cantina.senai.security.UsuarioAutenticado autenticado(TipoUsuario tipo) {
        return new br.com.cantina.senai.security.UsuarioAutenticado(criarUsuario(tipo));
    }
}
