package br.com.cantina.senai.dto.pedido;

import br.com.cantina.senai.model.itempedido.ItemPedido;
import br.com.cantina.senai.model.pedido.FormaPagamento;
import br.com.cantina.senai.model.pedido.Pedido;
import br.com.cantina.senai.model.pedido.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pedido completo para o painel do funcionario e para a tela do cliente.
 *
 * Os controllers antes devolviam a entidade Pedido direto, o que expunha o
 * modelo interno e quebrava na serializacao das associacoes lazy.
 */
public record DTODetalhamentoPedido(
        Long idPedido,
        Long idUsuario,
        String nomeUsuario,
        StatusPedido status,
        FormaPagamento formaPagamento,
        String observacao,
        LocalDateTime dataPedido,
        BigDecimal valorTotal,
        Integer totalItens,
        List<ItemDetalhado> itens
) {
        public record ItemDetalhado(
                Long idProduto,
                String nomeProduto,
                Integer quantidade,
                BigDecimal precoUnitario,
                BigDecimal subtotal
        ) {
                static ItemDetalhado de(ItemPedido item) {
                        return new ItemDetalhado(
                                item.getProduto().getIdProduto(),
                                item.getProduto().getNomeProduto(),
                                item.getQuantidade(),
                                item.getPrecoUnitario(),
                                item.getSubtotal()
                        );
                }
        }

        public DTODetalhamentoPedido(Pedido pedido) {
                this(
                        pedido.getIdPedido(),
                        pedido.getUsuario().getIdUsuario(),
                        pedido.getUsuario().getNome(),
                        pedido.getStatusPedido(),
                        pedido.getFormaPagamento(),
                        pedido.getObservacao(),
                        pedido.getDataPedido(),
                        pedido.getValorTotal(),
                        pedido.getTotalItens(),
                        pedido.getItens().stream().map(ItemDetalhado::de).toList()
                );
        }
}
