package br.com.cantina.senai.model.pedido;

import br.com.cantina.senai.model.itempedido.ItemPedido;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.model.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pedido feito por um usuario. O total e derivado dos itens e recalculado
 * sempre que a lista muda, entao nunca fica fora de sincronia com as linhas.
 */
@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long idPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    @Column(name = "data_pedido", nullable = false)
    private LocalDateTime dataPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pedido", nullable = false, length = 20)
    private StatusPedido statusPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Column(name = "observacao", length = 255)
    private String observacao;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    public Pedido(Usuario usuario, FormaPagamento formaPagamento, String observacao) {
        this.usuario = usuario;
        this.formaPagamento = formaPagamento;
        this.observacao = observacao;
        this.dataPedido = LocalDateTime.now();
        this.statusPedido = StatusPedido.CRIADO;
        this.valorTotal = BigDecimal.ZERO;
    }

    /**
     * Adiciona uma linha ou soma na existente, para que o mesmo produto pedido
     * duas vezes vire uma linha com quantidade 2 em vez de duas linhas.
     */
    public void adicionarItem(Produto produto, int quantidade) {
        ItemPedido existente = itens.stream()
                .filter(item -> item.getProduto().getIdProduto().equals(produto.getIdProduto()))
                .findFirst()
                .orElse(null);

        if (existente != null) {
            existente.setQuantidade(existente.getQuantidade() + quantidade);
        } else {
            itens.add(new ItemPedido(this, produto, quantidade));
        }
        recalcularTotal();
    }

    public void recalcularTotal() {
        this.valorTotal = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getTotalItens() {
        return itens.stream().mapToInt(ItemPedido::getQuantidade).sum();
    }

    public boolean pertenceA(Usuario candidato) {
        return usuario != null && candidato != null
                && usuario.getIdUsuario().equals(candidato.getIdUsuario());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pedido outro)) {
            return false;
        }
        return idPedido != null && idPedido.equals(outro.getIdPedido());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idPedido);
    }
}
