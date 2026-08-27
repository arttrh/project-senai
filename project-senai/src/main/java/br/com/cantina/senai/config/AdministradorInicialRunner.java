package br.com.cantina.senai.config;

import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;
import br.com.cantina.senai.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro ADMIN a partir de variaveis de ambiente, se ainda nao existir
 * nenhum.
 *
 * O usuario inicial nao vai numa migration de proposito: uma senha versionada
 * no repositorio e uma credencial publica. Aqui ela vem do ambiente e o hash
 * BCrypt e o unico valor que chega ao banco.
 */
@Component
public class AdministradorInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicialRunner.class);
    private static final int TAMANHO_MINIMO_SENHA = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PropriedadesAdminInicial propriedades;

    public AdministradorInicialRunner(UsuarioRepository usuarioRepository,
                                      PasswordEncoder passwordEncoder,
                                      PropriedadesAdminInicial propriedades) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.propriedades = propriedades;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByTipoUsuario(TipoUsuario.ADMIN)) {
            return;
        }

        if (!propriedades.estaConfigurado()) {
            log.warn("Nenhum ADMIN cadastrado e APP_ADMIN_EMAIL/APP_ADMIN_SENHA nao definidos. "
                    + "Defina as duas variaveis (veja o .env.example) para criar o administrador inicial.");
            return;
        }
        if (propriedades.senha().length() < TAMANHO_MINIMO_SENHA) {
            log.error("APP_ADMIN_SENHA tem menos de {} caracteres. ADMIN inicial nao criado.",
                    TAMANHO_MINIMO_SENHA);
            return;
        }
        if (usuarioRepository.existsByEmail(propriedades.emailNormalizado())) {
            log.warn("Ja existe uma conta com o e-mail de APP_ADMIN_EMAIL. ADMIN inicial nao criado.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome(propriedades.nome());
        admin.setCpf(propriedades.cpf());
        admin.setEmail(propriedades.emailNormalizado());
        admin.setSenha(passwordEncoder.encode(propriedades.senha()));
        admin.setTipoUsuario(TipoUsuario.ADMIN);
        admin.setAtivo(true);
        usuarioRepository.save(admin);

        log.info("ADMIN inicial criado para o e-mail {}", propriedades.emailNormalizado());
    }
}
