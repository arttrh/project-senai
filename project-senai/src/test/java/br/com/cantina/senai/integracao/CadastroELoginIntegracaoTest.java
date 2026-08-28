package br.com.cantina.senai.integracao;

import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Cadastro pela tela e pela API, e o login que passou a existir. */
@Transactional
class CadastroELoginIntegracaoTest extends BaseIntegracao {

    @Test
    @DisplayName("cadastro pela API cria a conta como USUARIO")
    void cadastroPelaApi() throws Exception {
        // O endpoint /api/usuarios respondia 404: a classe existia vazia, sem
        // @RestController, enquanto o Cadastro.js ja chamava essa rota.
        mockMvc.perform(post("/api/usuarios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana Souza","cpf":"529.982.247-25",
                                 "telefone":"(11) 97777-6666","email":"ana@senai.br",
                                 "senha":"senhaSegura1"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoUsuario").value("USUARIO"))
                .andExpect(jsonPath("$.email").value("ana@senai.br"));

        Usuario criado = usuarioRepository.findByEmail("ana@senai.br").orElseThrow();
        assertThat(criado.getSenha()).startsWith("$2");
        assertThat(criado.getCpf()).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("cadastro pela tela redireciona para o login")
    void cadastroPelaTela() throws Exception {
        // O formulario apontava para POST /home, rota inexistente: 405.
        mockMvc.perform(post("/usuario/cadastrar").with(csrf())
                        .param("nome", "Bruno Lima")
                        .param("cpf", "529.982.247-25")
                        .param("telefone", "(11) 96666-5555")
                        .param("email", "bruno@senai.br")
                        .param("senha", "senhaSegura1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?cadastrado"));

        assertThat(usuarioRepository.existsByEmail("bruno@senai.br")).isTrue();
    }

    @Test
    @DisplayName("cadastro com CPF invalido volta para o formulario com erro")
    void cadastroComCpfInvalido() throws Exception {
        // O regex antigo aceitava "12345678901" e recusava "123.456.789-01";
        // agora o CPF e conferido pelos digitos verificadores.
        mockMvc.perform(post("/usuario/cadastrar").with(csrf())
                        .param("nome", "Carlos Dias")
                        .param("cpf", "111.111.111-11")
                        .param("email", "carlos@senai.br")
                        .param("senha", "senhaSegura1"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"));

        assertThat(usuarioRepository.existsByEmail("carlos@senai.br")).isFalse();
    }

    @Test
    @DisplayName("cadastro nao aceita senha curta")
    void cadastroComSenhaCurta() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Dora Reis","cpf":"529.982.247-25",
                                 "email":"dora@senai.br","senha":"1234"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login com as credenciais certas autentica a sessao")
    void loginValido() throws Exception {
        Usuario usuario = criarUsuario(TipoUsuario.USUARIO);

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", usuario.getEmail())
                        .param("senha", SENHA_PADRAO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(authenticated());
    }

    @Test
    @DisplayName("funcionario cai direto no painel apos o login")
    void loginDeFuncionarioVaiParaOPainel() throws Exception {
        Usuario funcionario = criarUsuario(TipoUsuario.FUNCIONARIO);

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", funcionario.getEmail())
                        .param("senha", SENHA_PADRAO))
                .andExpect(redirectedUrl("/funcionario"))
                .andExpect(authenticated());
    }

    @Test
    @DisplayName("senha errada nao autentica")
    void loginComSenhaErrada() throws Exception {
        Usuario usuario = criarUsuario(TipoUsuario.USUARIO);

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", usuario.getEmail())
                        .param("senha", "senhaErrada123"))
                .andExpect(redirectedUrl("/login?erro"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("conta desativada nao consegue entrar")
    void contaDesativadaNaoEntra() throws Exception {
        Usuario usuario = criarUsuario(TipoUsuario.USUARIO);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", usuario.getEmail())
                        .param("senha", SENHA_PADRAO))
                .andExpect(unauthenticated());
    }
}
