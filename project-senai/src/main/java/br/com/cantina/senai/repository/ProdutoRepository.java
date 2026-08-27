package br.com.cantina.senai.repository;

import br.com.cantina.senai.model.produto.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByNomeProduto(String nomeProduto);

    boolean existsByNomeProduto(String nomeProduto);

    boolean existsByNomeProdutoAndIdProdutoNot(String nomeProduto, Long idProduto);

    List<Produto> findAllByProdutoAtivoTrueOrderByNomeProdutoAsc();
}
