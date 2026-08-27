package br.com.cantina.senai.dto.pedido;

import br.com.cantina.senai.model.pedido.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record DTOAtualizarStatusPedido(
        @NotNull(message = "Status e obrigatorio")
        StatusPedido status
) {
}
