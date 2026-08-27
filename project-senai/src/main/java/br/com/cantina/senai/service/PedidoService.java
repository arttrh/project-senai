package br.com.cantina.senai.service;

import br.com.cantina.senai.dto.pedido.*;
import br.com.cantina.senai.exception.*;
import br.com.cantina.senai.model.estoque.Estoque;
import br.com.cantina.senai.model.itempedido.ItemPedido;
import br.com.cantina.senai.model.pedido.Pedido;
import br.com.cantina.senai.model.pedido.StatusPedido;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.model.usuario.Usuario;
import br.com.cantina.senai.repository.EstoqueRepository;
import br.com.cantina.senai.repository.PedidoRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluxo de compra da cantina.
 *
 * Regras que este service garante:
 *  - um pedido nasce completo, com todos os itens, em uma unica transacao;
 *  - so entra item de produto ativo e com saldo suficiente;
 *  - o saldo cai na criacao e volta no cancelamento;
 *  - o preco e congelado no momento da compra;
 *  - o status so anda pelas transicoes declaradas em StatusPedido;
 *  - cliente enxerga e cancela apenas os proprios pedidos.
 */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    /** Status que o painel do funcionario trata como fila de trabalho. */
    private static final List<StatusPedido> EM_ANDAMENTO =
            List.of(StatusPedido.CRIADO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO);

    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;
    private final ProdutoService produtoService;
    private final UsuarioService usuarioService;

    private final Counter pedidosCriados;
    private final Counter pedidosCancelados;
    private final Counter falhasPorEstoque;

    public PedidoService(PedidoRepository pedidoRepository,
                         EstoqueRepository estoqueRepository,
                         ProdutoService produtoService,
                         UsuarioService usuarioService,
                         MeterRegistry meterRegistry) {
        this.pedidoRepository = pedidoRepository;
        this.estoqueRepository = estoqueRepository;
        this.produtoService = produtoService;
        this.usuarioService = usuarioService;

        this.pedidosCriados = Counter.builder("cantina.pedidos.criados")
                .description("Pedidos criados com sucesso")
                .register(meterRegistry);
        this.pedidosCancelados = Counter.builder("cantina.pedidos.cancelados")
                .description("Pedidos cancelados, com devolucao de estoque")
                .register(meterRegistry);
        this.falhasPorEstoque = Counter.builder("cantina.pedidos.recusados.estoque")
                .description("Pedidos recusados por falta de estoque")
                .register(meterRegistry);
    }

    /**
     * Cria o pedido inteiro de uma vez: valida todos os itens, baixa o estoque
     * de todos e so entao grava. Qualquer falha no meio derruba a transacao
     * inteira, entao nao existe pedido pela metade nem estoque baixado a toa.
     */
    @Transactional
    public DTODetalhamentoPedido criar(DTOCadastroPedido dados, Long idUsuario) {
        Usuario usuario = usuarioService.buscarEntidade(idUsuario);
        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Conta desativada nao pode fazer pedidos");
        }

        Map<Long, Integer> quantidadePorProduto = agruparItens(dados.itens());

        Pedido pedido = new Pedido(usuario, dados.formaPagamento(), dados.observacao());

        // Ordenado por id para que pedidos concorrentes travem as linhas de
        // estoque sempre na mesma ordem e nao formem deadlock.
        List<Long> idsOrdenados = quantidadePorProduto.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        for (Long idProduto : idsOrdenados) {
            int quantidade = quantidadePorProduto.get(idProduto);
            Produto produto = produtoService.buscarEntidade(idProduto);

            if (!produto.isProdutoAtivo()) {
                throw new RegraDeNegocioException(
                        "O produto '" + produto.getNomeProduto() + "' nao esta disponivel");
            }

            Estoque estoque = estoqueRepository.bloquearPorProduto(idProduto)
                    .orElseThrow(() -> new EstoqueNaoEncontradoException(
                            "Nao ha estoque cadastrado para o produto '" + produto.getNomeProduto() + "'"));

            if (!estoque.temSaldoPara(quantidade)) {
                falhasPorEstoque.increment();
                throw new EstoqueInsuficienteException(
                        produto.getNomeProduto(), quantidade, estoque.getQuantidade());
            }

            estoque.baixar(quantidade);
            pedido.adicionarItem(produto, quantidade);
        }

        pedidoRepository.save(pedido);
        pedidosCriados.increment();

        log.info("Pedido criado id={} usuario={} itens={} total={}",
                pedido.getIdPedido(), idUsuario, pedido.getTotalItens(), pedido.getValorTotal());
        return new DTODetalhamentoPedido(pedido);
    }

    /** Fila de trabalho do funcionario. */
    @Transactional(readOnly = true)
    public List<DTODetalhamentoPedido> listarEmAndamento() {
        return pedidoRepository.buscarPorStatusComItens(EM_ANDAMENTO).stream()
                .map(DTODetalhamentoPedido::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DTOListagemPedido> listarDoUsuario(Long idUsuario) {
        return pedidoRepository.buscarPorUsuarioComItens(idUsuario).stream()
                .map(DTOListagemPedido::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DTODetalhamentoPedido buscarPorId(Long idPedido) {
        return new DTODetalhamentoPedido(buscarEntidade(idPedido));
    }

    /**
     * Busca com checagem de dono: o cliente so ve o proprio pedido, funcionario
     * e admin veem qualquer um. Sem isso qualquer pessoa logada leria o pedido
     * dos outros so trocando o id na URL.
     */
    @Transactional(readOnly = true)
    public DTODetalhamentoPedido buscarParaUsuario(Long idPedido, Usuario solicitante) {
        Pedido pedido = buscarEntidade(idPedido);
        if (!solicitante.isFuncionario() && !pedido.pertenceA(solicitante)) {
            throw new AcessoNegadoException("Este pedido pertence a outro usuario");
        }
        return new DTODetalhamentoPedido(pedido);
    }

    /** Avanca o status respeitando as transicoes declaradas em StatusPedido. */
    @Transactional
    public DTODetalhamentoPedido alterarStatus(Long idPedido, StatusPedido novoStatus) {
        Pedido pedido = buscarEntidade(idPedido);
        StatusPedido atual = pedido.getStatusPedido();

        if (atual == novoStatus) {
            return new DTODetalhamentoPedido(pedido);
        }
        if (!atual.podeTransicionarPara(novoStatus)) {
            throw new TransicaoStatusInvalidaException(atual, novoStatus);
        }

        if (novoStatus == StatusPedido.CANCELADO) {
            devolverEstoque(pedido);
            pedidosCancelados.increment();
        }

        pedido.setStatusPedido(novoStatus);
        log.info("Pedido id={} mudou de {} para {}", idPedido, atual, novoStatus);
        return new DTODetalhamentoPedido(pedido);
    }

    /**
     * Cancelamento pelo proprio cliente, permitido so enquanto a cantina ainda
     * nao comecou a preparar.
     */
    @Transactional
    public DTODetalhamentoPedido cancelarComoCliente(Long idPedido, Usuario solicitante) {
        Pedido pedido = buscarEntidade(idPedido);

        if (!pedido.pertenceA(solicitante)) {
            throw new AcessoNegadoException("Este pedido pertence a outro usuario");
        }
        if (pedido.getStatusPedido() != StatusPedido.CRIADO) {
            throw new RegraDeNegocioException(
                    "O pedido ja esta em " + pedido.getStatusPedido()
                            + " e so pode ser cancelado pela cantina");
        }
        return alterarStatus(idPedido, StatusPedido.CANCELADO);
    }

    @Transactional(readOnly = true)
    public long contarEmAndamento() {
        return EM_ANDAMENTO.stream().mapToLong(pedidoRepository::countByStatusPedido).sum();
    }

    @Transactional(readOnly = true)
    public Pedido buscarEntidade(Long idPedido) {
        return pedidoRepository.buscarPorIdComItens(idPedido)
                .orElseThrow(() -> new PedidoNaoEncontradoException(
                        "Pedido nao encontrado. ID: " + idPedido));
    }

    /**
     * Devolve ao estoque tudo que o pedido tinha reservado.
     *
     * O metodo antigo marcava o pedido como CANCELADO e em seguida chamava
     * deleteById: o registro sumia, o estoque nunca voltava e o historico do
     * cliente perdia a compra.
     */
    private void devolverEstoque(Pedido pedido) {
        for (ItemPedido item : pedido.getItens()) {
            Long idProduto = item.getProduto().getIdProduto();
            estoqueRepository.bloquearPorProduto(idProduto).ifPresentOrElse(
                    estoque -> estoque.repor(item.getQuantidade()),
                    () -> log.warn("Pedido {} cancelado, mas o produto {} nao tem mais estoque cadastrado",
                            pedido.getIdPedido(), idProduto)
            );
        }
    }

    /**
     * Junta linhas repetidas do carrinho. Se o front mandar o mesmo produto em
     * dois itens, vira uma soma unica: assim a checagem de saldo enxerga o
     * total real em vez de aprovar cada metade separadamente.
     */
    private Map<Long, Integer> agruparItens(List<DTOItemPedido> itens) {
        Map<Long, Integer> agrupado = new LinkedHashMap<>();
        for (DTOItemPedido item : itens) {
            agrupado.merge(item.idProduto(), item.quantidade(), Integer::sum);
        }
        return agrupado;
    }
}
