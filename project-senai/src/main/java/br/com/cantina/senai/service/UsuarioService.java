package br.com.cantina.senai.service;

import br.com.cantina.senai.dto.usuario.*;
import br.com.cantina.senai.exception.RecursoDuplicadoException;
import br.com.cantina.senai.exception.RegraDeNegocioException;
import br.com.cantina.senai.exception.UsuarioNaoEncontradoException;
import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;
import br.com.cantina.senai.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de conta: quem pode existir, com que perfil e quem muda o que.
 *
 * Dependencias chegam pelo construtor (injecao explicita, sem @Autowired em
 * campo), o que deixa o service testavel sem subir contexto do Spring.
 */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Auto-cadastro. Sempre cria como USUARIO: o perfil nunca vem do cliente,
     * senao qualquer visitante se cadastraria como ADMIN.
     */
    @Transactional
    public DTODetalhamentoUsuario cadastrar(DTOCadastroUsuario dados) {
        String email = dados.emailNormalizado();
        String cpf = dados.cpfNormalizado();

        if (usuarioRepository.existsByEmail(email)) {
            throw new RecursoDuplicadoException("Ja existe uma conta com o e-mail " + email);
        }
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new RecursoDuplicadoException("Ja existe uma conta com este CPF");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dados.nome().trim());
        usuario.setCpf(cpf);
        usuario.setTelefone(dados.telefone());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dados.senha()));
        usuario.setTipoUsuario(dados.tipoPadrao());
        usuario.setAtivo(true);

        usuarioRepository.save(usuario);
        log.info("Usuario cadastrado id={} tipo={}", usuario.getIdUsuario(), usuario.getTipoUsuario());
        return new DTODetalhamentoUsuario(usuario);
    }

    /** Criacao com perfil escolhido. So a camada ADMIN chega aqui. */
    @Transactional
    public DTODetalhamentoUsuario cadastrarComTipo(DTOCadastroUsuario dados, TipoUsuario tipo) {
        DTODetalhamentoUsuario criado = cadastrar(dados);
        Usuario usuario = buscarEntidade(criado.idUsuario());
        usuario.setTipoUsuario(tipo);
        log.info("Usuario id={} criado com tipo={}", usuario.getIdUsuario(), tipo);
        return new DTODetalhamentoUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public List<DTOListagemUsuario> listar() {
        return usuarioRepository.findAll().stream()
                .map(DTOListagemUsuario::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DTODetalhamentoUsuario buscarPorId(Long idUsuario) {
        return new DTODetalhamentoUsuario(buscarEntidade(idUsuario));
    }

    @Transactional(readOnly = true)
    public DTODetalhamentoUsuario buscarPorEmail(String email) {
        return new DTODetalhamentoUsuario(buscarEntidadePorEmail(email));
    }

    @Transactional(readOnly = true)
    public Usuario buscarEntidadePorEmail(String email) {
        return usuarioRepository.findByEmail(email == null ? null : email.trim().toLowerCase())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario nao encontrado: " + email));
    }

    /**
     * Edicao do proprio perfil. Nao aceita troca de tipo: isso e um endpoint
     * separado e restrito a ADMIN.
     */
    @Transactional
    public DTODetalhamentoUsuario atualizar(Long idUsuario, DTOAtualizarUsuario dados) {
        Usuario usuario = buscarEntidade(idUsuario);

        if (dados.nome() != null && !dados.nome().isBlank()) {
            usuario.setNome(dados.nome().trim());
        }
        if (dados.telefone() != null) {
            usuario.setTelefone(dados.telefone());
        }
        if (dados.email() != null && !dados.email().isBlank()) {
            String novoEmail = dados.email().trim().toLowerCase();
            if (usuarioRepository.existsByEmailAndIdUsuarioNot(novoEmail, idUsuario)) {
                throw new RecursoDuplicadoException("Ja existe uma conta com o e-mail " + novoEmail);
            }
            usuario.setEmail(novoEmail);
        }
        return new DTODetalhamentoUsuario(usuario);
    }

    /** Troca de perfil: exclusiva de ADMIN, com trava contra remover o ultimo. */
    @Transactional
    public DTODetalhamentoUsuario alterarTipo(Long idUsuario, TipoUsuario novoTipo) {
        Usuario usuario = buscarEntidade(idUsuario);

        if (usuario.isAdmin() && novoTipo != TipoUsuario.ADMIN && contarAdmins() <= 1) {
            throw new RegraDeNegocioException("Nao e possivel rebaixar o unico ADMIN do sistema");
        }

        log.info("Tipo do usuario id={} alterado de {} para {}",
                idUsuario, usuario.getTipoUsuario(), novoTipo);
        usuario.setTipoUsuario(novoTipo);
        return new DTODetalhamentoUsuario(usuario);
    }

    @Transactional
    public void alterarSenha(Long idUsuario, DTOAlterarSenha dados) {
        Usuario usuario = buscarEntidade(idUsuario);

        if (!passwordEncoder.matches(dados.senhaAtual(), usuario.getSenha())) {
            throw new RegraDeNegocioException("Senha atual incorreta");
        }
        if (!dados.confirmacaoConfere()) {
            throw new RegraDeNegocioException("A confirmacao nao confere com a nova senha");
        }
        if (passwordEncoder.matches(dados.novaSenha(), usuario.getSenha())) {
            throw new RegraDeNegocioException("A nova senha deve ser diferente da atual");
        }

        usuario.setSenha(passwordEncoder.encode(dados.novaSenha()));
        log.info("Senha alterada para o usuario id={}", idUsuario);
    }

    /**
     * Desativa em vez de apagar: pedidos antigos apontam para o usuario e um
     * DELETE quebraria a chave estrangeira (o codigo anterior tentava exatamente
     * isso e virava erro 500).
     */
    @Transactional
    public void desativar(Long idUsuario) {
        Usuario usuario = buscarEntidade(idUsuario);

        if (usuario.isAdmin() && contarAdmins() <= 1) {
            throw new RegraDeNegocioException("Nao e possivel desativar o unico ADMIN do sistema");
        }

        usuario.setAtivo(false);
        log.info("Usuario id={} desativado", idUsuario);
    }

    @Transactional(readOnly = true)
    public Usuario buscarEntidade(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario nao encontrado. ID: " + idUsuario));
    }

    private long contarAdmins() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.isAdmin() && u.isAtivo())
                .count();
    }
}
