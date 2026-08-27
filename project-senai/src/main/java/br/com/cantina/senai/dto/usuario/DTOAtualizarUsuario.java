package br.com.cantina.senai.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Edicao do proprio perfil. Campos nulos ficam como estao (semantica de PATCH).
 *
 * tipoUsuario foi removido de proposito: no codigo anterior o proprio usuario
 * podia se promover a ADMIN por este endpoint. A troca de perfil agora tem um
 * endpoint separado, restrito a ADMIN.
 */
public record DTOAtualizarUsuario(
        @Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres")
        String nome,

        @Pattern(
                regexp = "^$|^\\(?\\d{2}\\)?[\\s-]?9?\\d{4}-?\\d{4}$",
                message = "Telefone deve estar no formato (11) 99999-9999"
        )
        String telefone,

        @Email(message = "E-mail invalido")
        @Size(max = 180)
        String email
) {
}
