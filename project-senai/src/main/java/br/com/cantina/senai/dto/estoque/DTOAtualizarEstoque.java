package br.com.cantina.senai.dto.estoque;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Ajuste manual de saldo pelo funcionario. */
public record DTOAtualizarEstoque(
        @NotNull(message = "Quantidade e obrigatoria")
        @Min(value = 0, message = "Quantidade nao pode ser negativa")
        Integer quantidade
) {
}
