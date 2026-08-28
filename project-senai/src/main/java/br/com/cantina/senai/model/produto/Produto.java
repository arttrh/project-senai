package br.com.cantina.senai.model.produto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item vendido na cantina. O preco fica aqui, mas o pedido guarda uma copia
 * do preco no momento da compra (ver ItemPedido) para que reajustes nao
 * alterem o valor de pedidos ja realizados.
 */
@Entity
@Table(name = "produto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto")
    private Long idProduto;

    @Column(name = "nome_produto", nullable = false, unique = true, length = 120)
    private String nomeProduto;

    @Column(name = "descricao_produto", length = 255)
    private String descricaoProduto;

    @Column(name = "preco", nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 20)
    private CategoriaProduto categoria = CategoriaProduto.LANCHE;

    @Column(name = "produto_ativo", nullable = false)
    private boolean produtoAtivo = true;

    /**
     * equals/hashCode por identidade persistente. Nao usar Lombok @Data aqui:
     * as associacoes bidirecionais gerariam recursao infinita.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Produto outro)) {
            return false;
        }
        return idProduto != null && idProduto.equals(outro.getIdProduto());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idProduto);
    }
}
