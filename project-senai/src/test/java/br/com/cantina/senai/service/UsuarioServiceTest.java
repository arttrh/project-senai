package br.com.cantina.senai.service;

import br.com.cantina.senai.dto.usuario.DTOAlterarSenha;
import br.com.cantina.senai.dto.usuario.DTOAtualizarUsuario;
import br.com.cantina.senai.dto.usuario.DTOCadastroUsuario;
import br.com.cantina.senai.dto.usuario.DTODetalhamentoUsuario;
import br.com.cantina.senai.exception.RecursoDuplicadoException;
import br.com.cantina.senai.exception.RegraDeNegocioException;
import br.com.cantina.senai.exception.UsuarioNaoEncontradoException;
import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;
import br.com.cantina.senai.repository.UsuarioRepository;
import br.com.cantina.senai.util.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private UsuarioService usuarioService;

    @BeforeEach
    void preparar() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(c -> c.getArgument(0));
    }

    private DTOCadastroUsuario cadastro() {
        return new DTOCadastroUsuario("Maria Silva", "529.982.247-25",
                "(11) 98888-7777", "Maria@SENAI.br", "senhaSegura1");
    }

    @Test
    @DisplayName("guarda a senha como hash, nunca em texto puro")
    void guardaSenhaComHash() {
        usuarioService.cadastrar(cadastro());

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());

        String armazenada = capturado.getValue().getSenha();
        assertThat(armazenada).isNotEqualTo("senhaSegura1").startsWith("$2");
        assertThat(passwordEncoder.matches("senhaSegura1", armazenada)).isTrue();
    }

    @Test
    @DisplayName("normaliza e-mail e CPF antes de gravar")
    void normalizaDados() {
        usuarioService.cadastrar(cadastro());

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());

        assertThat(capturado.getValue().getEmail()).isEqualTo("maria@senai.br");
        assertThat(capturado.getValue().getCpf()).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("auto-cadastro sempre cria como USUARIO")
    void autoCadastroNaoEscolhePerfil() {
        DTODetalhamentoUsuario criado = usuarioService.cadastrar(cadastro());

        assertThat(criado.tipoUsuario()).isEqualTo(TipoUsuario.USUARIO);
    }

    @Test
    @DisplayName("mascara o CPF na saida")
    void mascaraCpf() {
        DTODetalhamentoUsuario criado = usuarioService.cadastrar(cadastro());

        assertThat(criado.cpf()).isEqualTo("***.982.247-**").doesNotContain("52998224725");
    }

    @Test
    @DisplayName("recusa e-mail ja cadastrado")
    void recusaEmailDuplicado() {
        when(usuarioRepository.existsByEmail("maria@senai.br")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cadastrar(cadastro()))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    @DisplayName("recusa CPF ja cadastrado")
    void recusaCpfDuplicado() {
        when(usuarioRepository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cadastrar(cadastro()))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    @DisplayName("editar o proprio perfil nao muda o tipo de usuario")
    void edicaoDePerfilNaoEscalaPrivilegio() {
        // Regressao da falha central: o DTO de atualizacao carregava tipoUsuario
        // e o service o aplicava, entao qualquer USUARIO virava ADMIN sozinho.
        Usuario usuario = Fabrica.cliente(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        usuarioService.atualizar(1L, new DTOAtualizarUsuario("Novo Nome", null, null));

        assertThat(usuario.getTipoUsuario()).isEqualTo(TipoUsuario.USUARIO);
        assertThat(usuario.getNome()).isEqualTo("Novo Nome");
        assertThat(DTOAtualizarUsuario.class.getRecordComponents())
                .as("o DTO de auto-edicao nao pode expor tipoUsuario")
                .noneMatch(componente -> componente.getName().equals("tipoUsuario"));
    }

    @Test
    @DisplayName("campos nulos na atualizacao mantem o valor atual")
    void atualizacaoParcial() {
        Usuario usuario = Fabrica.cliente(1L);
        String telefoneOriginal = usuario.getTelefone();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        usuarioService.atualizar(1L, new DTOAtualizarUsuario(null, null, null));

        assertThat(usuario.getNome()).isEqualTo("Fulano de Tal");
        assertThat(usuario.getTelefone()).isEqualTo(telefoneOriginal);
    }

    @Test
    @DisplayName("recusa trocar para um e-mail que ja e de outra conta")
    void recusaEmailDeOutraConta() {
        Usuario usuario = Fabrica.cliente(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailAndIdUsuarioNot("ocupado@senai.br", 1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.atualizar(1L,
                new DTOAtualizarUsuario(null, null, "ocupado@senai.br")))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("ADMIN promove outro usuario pelo endpoint proprio")
    void adminPromoveUsuario() {
        Usuario usuario = Fabrica.cliente(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));

        usuarioService.alterarTipo(2L, TipoUsuario.FUNCIONARIO);

        assertThat(usuario.getTipoUsuario()).isEqualTo(TipoUsuario.FUNCIONARIO);
    }

    @Test
    @DisplayName("nao rebaixa o unico ADMIN do sistema")
    void protegeUltimoAdmin() {
        Usuario admin = Fabrica.usuario(1L, TipoUsuario.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findAll()).thenReturn(List.of(admin));

        assertThatThrownBy(() -> usuarioService.alterarTipo(1L, TipoUsuario.USUARIO))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("unico ADMIN");
    }

    @Test
    @DisplayName("desativa em vez de apagar, preservando o historico de pedidos")
    void desativaSemApagar() {
        Usuario usuario = Fabrica.cliente(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        usuarioService.desativar(1L);

        assertThat(usuario.isAtivo()).isFalse();
        verify(usuarioRepository, never()).delete(any());
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("troca de senha exige a senha atual correta")
    void trocaDeSenhaExigeSenhaAtual() {
        Usuario usuario = Fabrica.cliente(1L);
        usuario.setSenha(passwordEncoder.encode("senhaAntiga1"));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.alterarSenha(1L,
                new DTOAlterarSenha("errada", "senhaNova123", "senhaNova123")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("incorreta");
    }

    @Test
    @DisplayName("troca de senha exige confirmacao igual")
    void trocaDeSenhaExigeConfirmacao() {
        Usuario usuario = Fabrica.cliente(1L);
        usuario.setSenha(passwordEncoder.encode("senhaAntiga1"));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.alterarSenha(1L,
                new DTOAlterarSenha("senhaAntiga1", "senhaNova123", "outraCoisa")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("confirmacao");
    }

    @Test
    @DisplayName("troca de senha grava o novo hash")
    void trocaDeSenhaGravaHash() {
        Usuario usuario = Fabrica.cliente(1L);
        usuario.setSenha(passwordEncoder.encode("senhaAntiga1"));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        usuarioService.alterarSenha(1L,
                new DTOAlterarSenha("senhaAntiga1", "senhaNova123", "senhaNova123"));

        assertThat(passwordEncoder.matches("senhaNova123", usuario.getSenha())).isTrue();
    }

    @Test
    @DisplayName("usuario inexistente vira 404")
    void usuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId(99L))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }
}
