package br.com.cantina.senai.dto.estoque;

import br.com.cantina.senai.model.estoque.Estoque;

public record DTODetalhamentoEstoque(
        Long idEstoque,
        Long idProduto,
        String nomeProduto,
        Integer quantidade
) {
        public DTODetalhamentoEstoque(Estoque estoque) {
                this(
                        estoque.getIdEstoque(),
                        estoque.getProduto().getIdProduto(),
                        estoque.getProduto().getNomeProduto(),
                        estoque.getQuantidade()
                );
        }
}
