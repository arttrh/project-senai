package br.com.cantina.senai.dto.usuario;

import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.validation.CPF;
import jakarta.validation.constraints.*;

/**
 * Dados do auto-cadastro. Nao existe tipoUsuario aqui: quem se cadastra sozinho
 * entra sempre como USUARIO, e so um ADMIN promove alguem depois. Aceitar o
 * tipo vindo do formulario deixaria qualquer visitante virar ADMIN.
 */
public record DTOCadastroUsuario(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres")
        String nome,

        @NotBlank(message = "CPF e obrigatorio")
        @CPF
        String cpf,

        @Pattern(
                regexp = "^$|^\\(?\\d{2}\\)?[\\s-]?9?\\d{4}-?\\d{4}$",
                message = "Telefone deve estar no formato (11) 99999-9999"
        )
        String telefone,

        @NotBlank(message = "E-mail e obrigatorio")
        @Email(message = "E-mail invalido")
        @Size(max = 180)
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, max = 72, message = "Senha deve ter no minimo 8 caracteres")
        String senha
) {
        /** Normaliza o CPF para o formato guardado no banco. */
        public String cpfNormalizado() {
                return cpf == null ? null : cpf.replaceAll("[^0-9]", "");
        }

        public String emailNormalizado() {
                return email == null ? null : email.trim().toLowerCase();
        }

        /** O tipo nunca vem do cliente. */
        public TipoUsuario tipoPadrao() {
                return TipoUsuario.USUARIO;
        }
}
