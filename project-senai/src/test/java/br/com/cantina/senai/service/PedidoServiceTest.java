package br.com.cantina.senai.service;

import br.com.cantina.senai.dto.pedido.DTOCadastroPedido;
import br.com.cantina.senai.dto.pedido.DTODetalhamentoPedido;
import br.com.cantina.senai.dto.pedido.DTOItemPedido;
import br.com.cantina.senai.exception.*;
import br.com.cantina.senai.model.estoque.Estoque;
import br.com.cantina.senai.model.pedido.FormaPagamento;
import br.com.cantina.senai.model.pedido.Pedido;
import br.com.cantina.senai.model.pedido.StatusPedido;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;
import br.com.cantina.senai.repository.EstoqueRepository;
import br.com.cantina.senai.repository.PedidoRepository;
import br.com.cantina.senai.util.Fabrica;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regras do fluxo de compra. Cada teste aqui corresponde a uma regra que o
 * codigo anterior nao garantia.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private EstoqueRepository estoqueRepository;
    @Mock private ProdutoService produtoService;
    @Mock private UsuarioService usuarioService;

    private PedidoService pedidoService;

    private Usuario cliente;
    private Produto coxinha;
    private Produto suco;
    private Estoque estoqueCoxinha;
    private Estoque estoqueSuco;

    @BeforeEach
    void preparar() {
        pedidoService = new PedidoService(pedidoRepository, estoqueRepository,
                produtoService, usuarioService, new SimpleMeterRegistry());

        cliente = Fabrica.cliente(1L);
        coxinha = Fabrica.produto(10L, "Coxinha", "7.50");
        suco = Fabrica.produto(20L, "Suco Natural", "7.00");
        estoqueCoxinha = Fabrica.estoque(100L, coxinha, 10);
        estoqueSuco = Fabrica.estoque(200L, suco, 4);

        when(usuarioService.buscarEntidade(1L)).thenReturn(cliente);
        when(produtoService.buscarEntidade(10L)).thenReturn(coxinha);
        when(produtoService.buscarEntidade(20L)).thenReturn(suco);
        when(estoqueRepository.bloquearPorProduto(10L)).thenReturn(Optional.of(estoqueCoxinha));
        when(estoqueRepository.bloquearPorProduto(20L)).thenReturn(Optional.of(estoqueSuco));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private DTOCadastroPedido carrinho(DTOItemPedido... itens) {
        return new DTOCadastroPedido(List.of(itens), FormaPagamento.PIX, null);
    }

    @Nested
    @DisplayName("Criacao do pedido")
    class Criacao {

        @Test
        @DisplayName("baixa o estoque de todos os itens e calcula o total")
        void criaPedidoCompleto() {
            DTODetalhamentoPedido pedido = pedidoService.criar(
                    carrinho(new DTOItemPedido(10L, 2), new DTOItemPedido(20L, 1)), 1L);

            // 2 x 7,50 + 1 x 7,00
            assertThat(pedido.valorTotal()).isEqualByComparingTo("22.00");
            assertThat(pedido.totalItens()).isEqualTo(3);
            assertThat(pedido.status()).isEqualTo(StatusPedido.CRIADO);
            assertThat(estoqueCoxinha.getQuantidade()).isEqualTo(8);
            assertThat(estoqueSuco.getQuantidade()).isEqualTo(3);
        }

        @Test
        @DisplayName("congela o preco do produto no momento da compra")
        void congelaPreco() {
            DTODetalhamentoPedido pedido = pedidoService.criar(
                    carrinho(new DTOItemPedido(10L, 1)), 1L);

            // Reajuste posterior nao pode mexer no que ja foi vendido.
            coxinha.setPreco(new BigDecimal("9.90"));

            assertThat(pedido.itens().getFirst().precoUnitario()).isEqualByComparingTo("7.50");
            assertThat(pedido.valorTotal()).isEqualByComparingTo("7.50");
        }

        @Test
        @DisplayName("soma linhas repetidas do mesmo produto antes de checar o saldo")
        void agrupaLinhasRepetidas() {
            // 3 + 2 = 5 unidades, e o estoque do suco so tem 4.
            assertThatThrownBy(() -> pedidoService.criar(
                    carrinho(new DTOItemPedido(20L, 3), new DTOItemPedido(20L, 2)), 1L))
                    .isInstanceOf(EstoqueInsuficienteException.class);

            assertThat(estoqueSuco.getQuantidade())
                    .as("nada pode ser baixado quando o pedido e recusado")
                    .isEqualTo(4);
        }

        @Test
        @DisplayName("recusa quando falta estoque para um item")
        void recusaSemEstoque() {
            assertThatThrownBy(() -> pedidoService.criar(
                    carrinho(new DTOItemPedido(20L, 5)), 1L))
                    .isInstanceOf(EstoqueInsuficienteException.class)
                    .hasMessageContaining("Suco Natural");
        }

        @Test
        @DisplayName("nao baixa nada se um item posterior do carrinho falhar")
        void naoBaixaParcialmente() {
            // Este era o bug do fluxo antigo: cada item era uma transacao, entao
            // a falha do segundo deixava o primeiro ja debitado.
            assertThatThrownBy(() -> pedidoService.criar(
                    carrinho(new DTOItemPedido(10L, 1), new DTOItemPedido(20L, 99)), 1L))
                    .isInstanceOf(EstoqueInsuficienteException.class);

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("recusa produto fora do cardapio")
        void recusaProdutoInativo() {
            coxinha.setProdutoAtivo(false);

            assertThatThrownBy(() -> pedidoService.criar(
                    carrinho(new DTOItemPedido(10L, 1)), 1L))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("nao esta disponivel");
        }

        @Test
        @DisplayName("recusa pedido de conta desativada")
        void recusaContaDesativada() {
            cliente.setAtivo(false);

            assertThatThrownBy(() -> pedidoService.criar(
                    carrinho(new DTOItemPedido(10L, 1)), 1L))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("desativada");
        }

        @Test
        @DisplayName("recusa produto sem linha de estoque cadastrada")
        void recusaSemLinhaDeEstoque() {
            when(estoqueRepository.bloquearPorProduto(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.criar(
                    carrinho(new DTOItemPedido(10L, 1)), 1L))
                    .isInstanceOf(EstoqueNaoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("Ciclo de vida do status")
    class Status {

        private Pedido pedidoSalvo;

        @BeforeEach
        void criarPedido() {
            pedidoSalvo = Fabrica.pedido(50L, cliente);
            pedidoSalvo.adicionarItem(coxinha, 2);
            when(pedidoRepository.buscarPorIdComItens(50L)).thenReturn(Optional.of(pedidoSalvo));
        }

        @Test
        @DisplayName("avanca pela sequencia valida")
        void avancaStatus() {
            assertThat(pedidoService.alterarStatus(50L, StatusPedido.EM_PREPARACAO).status())
                    .isEqualTo(StatusPedido.EM_PREPARACAO);
            assertThat(pedidoService.alterarStatus(50L, StatusPedido.PRONTO).status())
                    .isEqualTo(StatusPedido.PRONTO);
            assertThat(pedidoService.alterarStatus(50L, StatusPedido.FINALIZADO).status())
                    .isEqualTo(StatusPedido.FINALIZADO);
        }

        @Test
        @DisplayName("recusa pular etapas")
        void recusaPularEtapa() {
            assertThatThrownBy(() -> pedidoService.alterarStatus(50L, StatusPedido.FINALIZADO))
                    .isInstanceOf(TransicaoStatusInvalidaException.class);
        }

        @Test
        @DisplayName("recusa mexer em pedido ja encerrado")
        void recusaReabrirEncerrado() {
            pedidoSalvo.setStatusPedido(StatusPedido.FINALIZADO);

            assertThatThrownBy(() -> pedidoService.alterarStatus(50L, StatusPedido.EM_PREPARACAO))
                    .isInstanceOf(TransicaoStatusInvalidaException.class);
        }

        @Test
        @DisplayName("cancelamento devolve o estoque e preserva o pedido")
        void cancelamentoDevolveEstoque() {
            int antes = estoqueCoxinha.getQuantidade();

            DTODetalhamentoPedido cancelado =
                    pedidoService.alterarStatus(50L, StatusPedido.CANCELADO);

            assertThat(cancelado.status()).isEqualTo(StatusPedido.CANCELADO);
            assertThat(estoqueCoxinha.getQuantidade()).isEqualTo(antes + 2);
            // O metodo antigo chamava deleteById depois de marcar CANCELADO.
            verify(pedidoRepository, never()).deleteById(any());
            verify(pedidoRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Acesso aos pedidos")
    class Acesso {

        private Pedido pedidoDoCliente;

        @BeforeEach
        void criarPedido() {
            pedidoDoCliente = Fabrica.pedido(60L, cliente);
            when(pedidoRepository.buscarPorIdComItens(60L)).thenReturn(Optional.of(pedidoDoCliente));
        }

        @Test
        @DisplayName("o dono enxerga o proprio pedido")
        void donoEnxerga() {
            assertThat(pedidoService.buscarParaUsuario(60L, cliente).idPedido()).isEqualTo(60L);
        }

        @Test
        @DisplayName("outro cliente nao enxerga o pedido alheio")
        void outroClienteNaoEnxerga() {
            Usuario intruso = Fabrica.cliente(2L);

            assertThatThrownBy(() -> pedidoService.buscarParaUsuario(60L, intruso))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        @DisplayName("funcionario enxerga qualquer pedido")
        void funcionarioEnxergaTudo() {
            Usuario funcionario = Fabrica.usuario(3L, TipoUsuario.FUNCIONARIO);

            assertThat(pedidoService.buscarParaUsuario(60L, funcionario).idPedido()).isEqualTo(60L);
        }

        @Test
        @DisplayName("cliente so cancela enquanto o preparo nao comecou")
        void clienteCancelaAntesDoPreparo() {
            pedidoDoCliente.setStatusPedido(StatusPedido.EM_PREPARACAO);

            assertThatThrownBy(() -> pedidoService.cancelarComoCliente(60L, cliente))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("cantina");
        }

        @Test
        @DisplayName("cliente nao cancela pedido de outro")
        void clienteNaoCancelaPedidoAlheio() {
            assertThatThrownBy(() -> pedidoService.cancelarComoCliente(60L, Fabrica.cliente(2L)))
                    .isInstanceOf(AcessoNegadoException.class);
        }
    }

    @Test
    @DisplayName("pedido inexistente vira 404, nao NullPointerException")
    void pedidoInexistente() {
        when(pedidoRepository.buscarPorIdComItens(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.buscarPorId(999L))
                .isInstanceOf(PedidoNaoEncontradoException.class);
    }
}
