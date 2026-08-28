package br.com.cantina.senai.dto.produto;

import br.com.cantina.senai.model.produto.CategoriaProduto;
import br.com.cantina.senai.model.produto.Produto;

import java.math.BigDecimal;

/**
 * Item do cardapio como o front precisa dele: com preco, categoria e saldo.
 *
 * A listagem antiga nao trazia preco nenhum, e por isso a home mostrava
 * "R$ --" fixo e o carrinho somava sempre zero.
 */
public record DTOListagemProduto(
        Long idProduto,
        String nomeProduto,
        String descricaoProduto,
        BigDecimal preco,
        CategoriaProduto categoria,
        Integer quantidadeEstoque,
        boolean disponivel
) {
        public static DTOListagemProduto de(Produto produto, Integer quantidadeEstoque) {
                int saldo = quantidadeEstoque == null ? 0 : quantidadeEstoque;
                return new DTOListagemProduto(
                        produto.getIdProduto(),
                        produto.getNomeProduto(),
                        produto.getDescricaoProduto(),
                        produto.getPreco(),
                        produto.getCategoria(),
                        saldo,
                        produto.isProdutoAtivo() && saldo > 0
                );
        }
}
