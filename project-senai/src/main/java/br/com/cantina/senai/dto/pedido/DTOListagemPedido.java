package br.com.cantina.senai.dto.pedido;

import br.com.cantina.senai.model.pedido.Pedido;
import br.com.cantina.senai.model.pedido.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DTOListagemPedido(
        Long idPedido,
        String nomeUsuario,
        StatusPedido status,
        LocalDateTime dataPedido,
        Integer totalItens,
        BigDecimal valorTotal
) {
        public DTOListagemPedido(Pedido pedido) {
                this(
                        pedido.getIdPedido(),
                        pedido.getUsuario().getNome(),
                        pedido.getStatusPedido(),
                        pedido.getDataPedido(),
                        pedido.getTotalItens(),
                        pedido.getValorTotal()
                );
        }
}
