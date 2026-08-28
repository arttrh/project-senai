package br.com.cantina.senai.repository;

import br.com.cantina.senai.model.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    /** Usado na atualizacao: o proprio usuario pode manter o seu email. */
    boolean existsByEmailAndIdUsuarioNot(String email, Long idUsuario);

    boolean existsByTipoUsuario(br.com.cantina.senai.model.usuario.TipoUsuario tipoUsuario);
}
