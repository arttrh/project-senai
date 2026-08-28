package br.com.cantina.senai.dto.usuario;

import br.com.cantina.senai.model.usuario.TipoUsuario;
import jakarta.validation.constraints.NotNull;

/** Promocao/rebaixamento de perfil. Endpoint exclusivo de ADMIN. */
public record DTOAlterarTipoUsuario(
        @NotNull(message = "Tipo de usuario e obrigatorio")
        TipoUsuario tipoUsuario
) {
}
