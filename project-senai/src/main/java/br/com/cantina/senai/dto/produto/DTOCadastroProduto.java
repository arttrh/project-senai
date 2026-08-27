package br.com.cantina.senai.dto.produto;

import br.com.cantina.senai.model.produto.CategoriaProduto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Cadastro de um item do cardapio. A quantidade inicial cria o estoque junto,
 * porque produto sem linha de estoque nunca pode ser vendido.
 */
public record DTOCadastroProduto(
        @NotBlank(message = "Nome do produto e obrigatorio")
        @Size(min = 2, max = 120)
        String nomeProduto,

        @Size(max = 255)
        String descricaoProduto,

        @NotNull(message = "Preco e obrigatorio")
        @DecimalMin(value = "0.01", message = "Preco deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Preco invalido")
        BigDecimal preco,

        @NotNull(message = "Categoria e obrigatoria")
        CategoriaProduto categoria,

        @NotNull(message = "Quantidade inicial e obrigatoria")
        @Min(value = 0, message = "Quantidade inicial nao pode ser negativa")
        Integer quantidadeInicial
) {
}
