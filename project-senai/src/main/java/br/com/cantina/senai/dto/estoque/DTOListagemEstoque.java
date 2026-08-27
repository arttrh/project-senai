package br.com.cantina.senai.dto.estoque;

import br.com.cantina.senai.model.estoque.Estoque;

import java.math.BigDecimal;

public record DTOListagemEstoque(
        Long idEstoque,
        Long idProduto,
        String nomeProduto,
        String descricaoProduto,
        BigDecimal preco,
        Integer quantidade,
        boolean produtoAtivo
) {
        public DTOListagemEstoque(Estoque estoque) {
                this(
                        estoque.getIdEstoque(),
                        estoque.getProduto().getIdProduto(),
                        estoque.getProduto().getNomeProduto(),
                        estoque.getProduto().getDescricaoProduto(),
                        estoque.getProduto().getPreco(),
                        estoque.getQuantidade(),
                        estoque.getProduto().isProdutoAtivo()
                );
        }
}
