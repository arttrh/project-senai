package br.com.cantina.senai.security;

import br.com.cantina.senai.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carrega a conta pelo e-mail, que e o "username" do login. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return usuarioRepository.findByEmail(email == null ? null : email.trim().toLowerCase())
                .map(UsuarioAutenticado::new)
                // Mensagem generica de proposito: dizer "e-mail nao existe"
                // permitiria enumerar contas cadastradas.
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas"));
    }
}
