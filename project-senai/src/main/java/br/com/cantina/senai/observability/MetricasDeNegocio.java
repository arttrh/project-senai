package br.com.cantina.senai.observability;

import br.com.cantina.senai.model.pedido.StatusPedido;
import br.com.cantina.senai.repository.EstoqueRepository;
import br.com.cantina.senai.repository.PedidoRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Medidores do dominio, para o /actuator/prometheus responder perguntas de
 * operacao e nao so de JVM: quantos pedidos estao na fila e quantos produtos
 * ja zeraram o estoque.
 *
 * Os dois medem com uma consulta de contagem cada. Contar em memoria (buscar
 * todas as linhas e filtrar no stream) custaria uma varredura completa da
 * tabela a cada coleta do Prometheus, que acontece a cada poucos segundos.
 *
 * Tambem nao ha @Transactional aqui: as lambdas dos gauges chamam os metodos
 * por this, sem passar pelo proxy do Spring, entao a anotacao nao teria efeito
 * nenhum e so prometeria uma transacao que nunca abriria. Cada consulta abre a
 * sua, que e o suficiente para uma leitura unica.
 */
@Configuration
public class MetricasDeNegocio {

    /** Status que ocupam a fila de trabalho da cantina. */
    private static final List<StatusPedido> NA_FILA =
            List.of(StatusPedido.CRIADO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO);

    public MetricasDeNegocio(MeterRegistry registry,
                             PedidoRepository pedidoRepository,
                             EstoqueRepository estoqueRepository) {

        Gauge.builder("cantina.pedidos.fila", () -> pedidoRepository.countByStatusPedidoIn(NA_FILA))
                .description("Pedidos aguardando preparo ou retirada")
                .register(registry);

        Gauge.builder("cantina.produtos.sem.estoque", estoqueRepository::contarSemSaldo)
                .description("Produtos com saldo zerado")
                .register(registry);
    }
}
