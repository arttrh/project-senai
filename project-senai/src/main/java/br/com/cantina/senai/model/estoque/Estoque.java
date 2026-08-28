package br.com.cantina.senai.model.estoque;

import br.com.cantina.senai.model.produto.Produto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Saldo de um produto. Relacao 1:1 com Produto (garantida por UNIQUE na
 * migration), e @Version protege contra duas compras simultaneas do mesmo
 * item baixarem o estoque em cima uma da outra (lost update).
 */
@Entity
@Table(name = "estoque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estoque")
    private Long idEstoque;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_produto", nullable = false, unique = true)
    private Produto produto;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    public Estoque(Produto produto, Integer quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public boolean temSaldoPara(int quantidadeDesejada) {
        return quantidade != null && quantidade >= quantidadeDesejada;
    }

    /** Baixa o saldo. Quem chama e responsavel por validar antes. */
    public void baixar(int quantidadeBaixa) {
        this.quantidade = this.quantidade - quantidadeBaixa;
    }

    /** Devolve o saldo, usado quando um pedido e cancelado. */
    public void repor(int quantidadeReposta) {
        this.quantidade = this.quantidade + quantidadeReposta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Estoque outro)) {
            return false;
        }
        return idEstoque != null && idEstoque.equals(outro.getIdEstoque());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idEstoque);
    }
}
