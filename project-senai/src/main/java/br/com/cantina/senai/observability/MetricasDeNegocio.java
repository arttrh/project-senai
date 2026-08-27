package br.com.cantina.senai.observability;

import br.com.cantina.senai.model.pedido.StatusPedido;
import br.com.cantina.senai.repository.EstoqueRepository;
import br.com.cantina.senai.repository.PedidoRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Medidores do dominio, para o /actuator/prometheus responder perguntas de
 * operacao e nao so de JVM: quantos pedidos estao na fila e quantos produtos
 * ja zeraram o estoque.
 */
@Configuration
public class MetricasDeNegocio {

    public MetricasDeNegocio(MeterRegistry registry,
                             PedidoRepository pedidoRepository,
                             EstoqueRepository estoqueRepository) {

        Gauge.builder("cantina.pedidos.fila", () -> contarFila(pedidoRepository))
                .description("Pedidos aguardando preparo ou retirada")
                .register(registry);

        Gauge.builder("cantina.produtos.sem.estoque", () -> contarSemEstoque(estoqueRepository))
                .description("Produtos com saldo zerado")
                .register(registry);
    }

    @Transactional(readOnly = true)
    long contarFila(PedidoRepository pedidoRepository) {
        return pedidoRepository.countByStatusPedido(StatusPedido.CRIADO)
                + pedidoRepository.countByStatusPedido(StatusPedido.EM_PREPARACAO)
                + pedidoRepository.countByStatusPedido(StatusPedido.PRONTO);
    }

    @Transactional(readOnly = true)
    long contarSemEstoque(EstoqueRepository estoqueRepository) {
        return estoqueRepository.findAll().stream()
                .filter(estoque -> estoque.getQuantidade() != null && estoque.getQuantidade() <= 0)
                .count();
    }
}
