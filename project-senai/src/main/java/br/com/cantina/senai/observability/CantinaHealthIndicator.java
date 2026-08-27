package br.com.cantina.senai.observability;

import br.com.cantina.senai.repository.EstoqueRepository;
import br.com.cantina.senai.repository.ProdutoRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Health check de dominio, alem do de banco que o Actuator ja traz: uma
 * cantina sem nenhum item vendavel esta "de pe" mas nao esta funcionando.
 * Reporta DOWN nesse caso para o orquestrador nao mandar trafego a toa.
 */
@Component("cantina")
public class CantinaHealthIndicator implements HealthIndicator {

    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;

    public CantinaHealthIndicator(ProdutoRepository produtoRepository,
                                  EstoqueRepository estoqueRepository) {
        this.produtoRepository = produtoRepository;
        this.estoqueRepository = estoqueRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Health health() {
        long ativos = produtoRepository.findAllByProdutoAtivoTrueOrderByNomeProdutoAsc().size();
        long comSaldo = estoqueRepository.findAll().stream()
                .filter(e -> e.getQuantidade() != null && e.getQuantidade() > 0)
                .count();

        Health.Builder status = (ativos > 0 && comSaldo > 0) ? Health.up() : Health.down();
        return status
                .withDetail("produtosAtivos", ativos)
                .withDetail("produtosComSaldo", comSaldo)
                .build();
    }
}
