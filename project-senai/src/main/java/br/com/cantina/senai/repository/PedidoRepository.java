package br.com.cantina.senai.repository;

import br.com.cantina.senai.model.pedido.Pedido;
import br.com.cantina.senai.model.pedido.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Carrega usuario, itens e produtos de uma vez. Sem isso a serializacao do
     * pedido estoura LazyInitializationException fora da transacao, que era o
     * que acontecia no painel do funcionario.
     */
    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH p.itens item
            LEFT JOIN FETCH item.produto
            WHERE p.statusPedido IN :status
            ORDER BY p.dataPedido ASC
            """)
    List<Pedido> buscarPorStatusComItens(Collection<StatusPedido> status);

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH p.itens item
            LEFT JOIN FETCH item.produto
            WHERE p.idPedido = :idPedido
            """)
    Optional<Pedido> buscarPorIdComItens(Long idPedido);

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.usuario u
            LEFT JOIN FETCH p.itens item
            LEFT JOIN FETCH item.produto
            WHERE u.idUsuario = :idUsuario
            ORDER BY p.dataPedido DESC
            """)
    List<Pedido> buscarPorUsuarioComItens(Long idUsuario);

    long countByStatusPedido(StatusPedido statusPedido);

    /**
     * Conta a fila inteira numa consulta so. Somar tres countByStatusPedido
     * separados leria cada status numa transacao diferente, podendo devolver um
     * retrato inconsistente enquanto pedidos mudam de estado.
     */
    long countByStatusPedidoIn(Collection<StatusPedido> status);
}
