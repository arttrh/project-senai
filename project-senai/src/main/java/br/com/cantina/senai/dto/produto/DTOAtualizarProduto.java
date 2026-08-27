package br.com.cantina.senai.dto.produto;

import br.com.cantina.senai.model.produto.CategoriaProduto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Atualizacao parcial: campo nulo mantem o valor atual.
 *
 * As anotacoes antigas eram @NotBlank enquanto o service tratava os campos
 * como opcionais, entao a validacao e o comportamento se contradiziam.
 */
public record DTOAtualizarProduto(
        @Size(min = 2, max = 120)
        String nomeProduto,

        @Size(max = 255)
        String descricaoProduto,

        @DecimalMin(value = "0.01", message = "Preco deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Preco invalido")
        BigDecimal preco,

        CategoriaProduto categoria,

        Boolean produtoAtivo
) {
}
