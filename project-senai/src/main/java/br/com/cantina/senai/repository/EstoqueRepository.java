package br.com.cantina.senai.repository;

import br.com.cantina.senai.model.estoque.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByProduto_IdProduto(Long idProduto);

    boolean existsByProduto_IdProduto(Long idProduto);

    /** Traz o produto junto: a listagem le nome e descricao de cada linha. */
    @Query("SELECT e FROM Estoque e JOIN FETCH e.produto ORDER BY e.produto.nomeProduto ASC")
    List<Estoque> buscarTodosComProduto();

    @Query("SELECT e FROM Estoque e JOIN FETCH e.produto WHERE e.idEstoque = :idEstoque")
    Optional<Estoque> buscarPorIdComProduto(Long idEstoque);

    /**
     * Trava a linha de estoque (SELECT ... FOR UPDATE) para a baixa de venda.
     *
     * Sem isso, dois pedidos simultaneos do mesmo produto leem o mesmo saldo e
     * ambos passam na checagem, deixando o estoque negativo. O @Version da
     * entidade cobre o caso geral; aqui a trava evita ate a tentativa perdida.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Estoque e WHERE e.produto.idProduto = :idProduto")
    Optional<Estoque> bloquearPorProduto(Long idProduto);
}
