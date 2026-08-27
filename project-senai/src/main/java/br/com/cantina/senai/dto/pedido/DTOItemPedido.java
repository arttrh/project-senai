package br.com.cantina.senai.dto.pedido;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Uma linha do carrinho enviada pelo front. */
public record DTOItemPedido(
        @NotNull(message = "Produto e obrigatorio")
        Long idProduto,

        @NotNull(message = "Quantidade e obrigatoria")
        @Min(value = 1, message = "Quantidade deve ser no minimo 1")
        Integer quantidade
) {
}
