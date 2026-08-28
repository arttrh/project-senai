package br.com.cantina.senai.dto.produto;

import br.com.cantina.senai.model.produto.CategoriaProduto;
import br.com.cantina.senai.model.produto.Produto;

import java.math.BigDecimal;

public record DTODetalhamentoProduto(
        Long idProduto,
        String nomeProduto,
        String descricaoProduto,
        BigDecimal preco,
        CategoriaProduto categoria,
        boolean produtoAtivo
) {
        public DTODetalhamentoProduto(Produto produto) {
                this(
                        produto.getIdProduto(),
                        produto.getNomeProduto(),
                        produto.getDescricaoProduto(),
                        produto.getPreco(),
                        produto.getCategoria(),
                        produto.isProdutoAtivo()
                );
        }
}
