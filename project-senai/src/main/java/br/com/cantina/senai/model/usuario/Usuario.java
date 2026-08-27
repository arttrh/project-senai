package br.com.cantina.senai.model.usuario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Conta do sistema. O tipo define o que a pessoa pode fazer e so pode ser
 * alterado por um ADMIN (ver UsuarioService), nunca pelo proprio dono.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "cpf", nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    /** Sempre hash BCrypt. Nunca exposto em nenhum DTO de saida. */
    @Column(name = "senha", nullable = false, length = 100)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 20)
    private TipoUsuario tipoUsuario;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    public boolean isFuncionario() {
        return tipoUsuario == TipoUsuario.FUNCIONARIO || tipoUsuario == TipoUsuario.ADMIN;
    }

    public boolean isAdmin() {
        return tipoUsuario == TipoUsuario.ADMIN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario outro)) {
            return false;
        }
        return idUsuario != null && idUsuario.equals(outro.getIdUsuario());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idUsuario);
    }
}
