package br.com.cantina.senai.dto.estoque;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DTOCadastroEstoque(
        @NotNull(message = "Produto e obrigatorio")
        Long idProduto,

        @NotNull(message = "Quantidade e obrigatoria")
        @Min(value = 0, message = "Quantidade nao pode ser negativa")
        Integer quantidade
) {
}
