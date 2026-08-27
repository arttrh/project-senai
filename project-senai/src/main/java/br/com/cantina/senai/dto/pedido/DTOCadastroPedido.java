package br.com.cantina.senai.dto.pedido;

import br.com.cantina.senai.model.pedido.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pedido completo em uma unica requisicao.
 *
 * O fluxo antigo criava um pedido vazio e depois adicionava cada item por outra
 * chamada, entao um carrinho de 3 itens virava 4 transacoes independentes: se a
 * terceira falhasse por falta de estoque, as duas primeiras ja tinham baixado o
 * saldo e o pedido ficava pela metade. Aqui o carrinho inteiro entra ou nada
 * entra.
 */
public record DTOCadastroPedido(
        @NotEmpty(message = "O pedido precisa de pelo menos um item")
        @Size(max = 50, message = "Pedido com itens demais")
        @Valid
        List<DTOItemPedido> itens,

        @NotNull(message = "Forma de pagamento e obrigatoria")
        FormaPagamento formaPagamento,

        @Size(max = 255, message = "Observacao muito longa")
        String observacao
) {
}
