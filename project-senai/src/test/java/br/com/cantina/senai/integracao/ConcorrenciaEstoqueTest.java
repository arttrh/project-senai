package br.com.cantina.senai.integracao;

import br.com.cantina.senai.dto.pedido.DTOCadastroPedido;
import br.com.cantina.senai.dto.pedido.DTOItemPedido;
import br.com.cantina.senai.model.pedido.FormaPagamento;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;
import br.com.cantina.senai.service.PedidoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corrida de estoque.
 *
 * Sem trava, dois pedidos simultaneos do mesmo produto leem o mesmo saldo,
 * ambos passam na checagem e ambos gravam a subtracao em cima da leitura
 * antiga: o estoque termina negativo e a cantina vende o que nao tem.
 *
 * Este teste NAO usa @Transactional de proposito: precisa de transacoes reais
 * em threads separadas para exercitar a trava pessimista de verdade.
 */
class ConcorrenciaEstoqueTest extends BaseIntegracao {

    @Autowired private PedidoService pedidoService;

    private Long idProdutoCriado;

    @AfterEach
    void limpar() {
        // Sem rollback automatico, a limpeza e manual.
        pedidoRepository.deleteAll();
        estoqueRepository.deleteAll();
        produtoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("20 pedidos simultaneos de 1 unidade nunca deixam o estoque negativo")
    void naoVendeAlemDoEstoque() throws Exception {
        final int saldoInicial = 10;
        final int tentativas = 20;

        Produto produto = criarProduto("Coxinha Concorrente", "7.50", saldoInicial);
        idProdutoCriado = produto.getIdProduto();

        List<Usuario> clientes = new java.util.ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            clientes.add(criarUsuario(TipoUsuario.USUARIO));
        }

        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger recusas = new AtomicInteger();

        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch chegada = new CountDownLatch(tentativas);

        try (ExecutorService executor = Executors.newFixedThreadPool(tentativas)) {
            for (Usuario cliente : clientes) {
                executor.submit(() -> {
                    try {
                        largada.await();
                        pedidoService.criar(new DTOCadastroPedido(
                                List.of(new DTOItemPedido(idProdutoCriado, 1)),
                                FormaPagamento.PIX, null), cliente.getIdUsuario());
                        sucessos.incrementAndGet();
                    } catch (Exception e) {
                        // Falta de estoque ou conflito de concorrencia: ambos
                        // sao recusas legitimas, o que nao pode e passar.
                        recusas.incrementAndGet();
                    } finally {
                        chegada.countDown();
                    }
                });
            }

            largada.countDown();
            assertThat(chegada.await(60, TimeUnit.SECONDS))
                    .as("as threads precisam terminar sem travar")
                    .isTrue();
        }

        int saldoFinal = estoqueRepository.findByProduto_IdProduto(idProdutoCriado)
                .orElseThrow().getQuantidade();

        assertThat(saldoFinal)
                .as("o estoque nunca pode ficar negativo")
                .isNotNegative();
        assertThat(sucessos.get())
                .as("no maximo %d pedidos podiam ser aceitos", saldoInicial)
                .isLessThanOrEqualTo(saldoInicial);
        assertThat(sucessos.get() + saldoFinal)
                .as("vendidos + saldo restante tem de fechar com o saldo inicial")
                .isEqualTo(saldoInicial);
        assertThat(sucessos.get() + recusas.get()).isEqualTo(tentativas);
        assertThat(recusas.get())
                .as("com 20 tentativas para 10 unidades, alguem tem de ser recusado")
                .isPositive();
        System.out.printf("[concorrencia] aceitos=%d recusados=%d saldoFinal=%d%n",
                sucessos.get(), recusas.get(), saldoFinal);
        assertThat(pedidoRepository.count())
                .as("cada sucesso grava exatamente um pedido")
                .isEqualTo(sucessos.get());
    }
}
