package br.com.cantina.senai.model.itempedido;

import br.com.cantina.senai.model.pedido.Pedido;
import br.com.cantina.senai.model.produto.Produto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Linha de um pedido. Substitui a antiga QuantidadeProduto, que usava @Data
 * e entrava em recursao infinita no toString/equals por causa das associacoes
 * bidirecionais.
 *
 * O precoUnitario e uma copia do preco do produto no instante da compra: se a
 * cantina reajustar o cardapio depois, o historico de pedidos nao muda.
 */
@Entity
@Table(name = "item_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item_pedido")
    private Long idItemPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_produto", nullable = false)
    private Produto produto;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    public ItemPedido(Pedido pedido, Produto produto, Integer quantidade) {
        this.pedido = pedido;
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPreco();
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemPedido outro)) {
            return false;
        }
        return idItemPedido != null && idItemPedido.equals(outro.getIdItemPedido());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idItemPedido);
    }
}
