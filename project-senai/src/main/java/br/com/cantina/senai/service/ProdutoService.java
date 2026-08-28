package br.com.cantina.senai.service;

import br.com.cantina.senai.dto.produto.*;
import br.com.cantina.senai.exception.ProdutoNaoEncontradoException;
import br.com.cantina.senai.exception.RecursoDuplicadoException;
import br.com.cantina.senai.model.estoque.Estoque;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.repository.EstoqueRepository;
import br.com.cantina.senai.repository.ProdutoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Cardapio da cantina. */
@Service
public class ProdutoService {

    private static final Logger log = LoggerFactory.getLogger(ProdutoService.class);

    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;

    public ProdutoService(ProdutoRepository produtoRepository, EstoqueRepository estoqueRepository) {
        this.produtoRepository = produtoRepository;
        this.estoqueRepository = estoqueRepository;
    }

    /**
     * Cria o produto e ja abre a linha de estoque. Produto sem estoque nunca
     * poderia ser vendido, entao criar os dois juntos evita um estado invalido.
     */
    @Transactional
    public DTODetalhamentoProduto cadastrar(DTOCadastroProduto dados) {
        String nome = dados.nomeProduto().trim();
        if (produtoRepository.existsByNomeProduto(nome)) {
            throw new RecursoDuplicadoException("Ja existe um produto chamado '" + nome + "'");
        }

        Produto produto = new Produto();
        produto.setNomeProduto(nome);
        produto.setDescricaoProduto(dados.descricaoProduto());
        produto.setPreco(dados.preco());
        produto.setCategoria(dados.categoria());
        produto.setProdutoAtivo(true);
        produtoRepository.save(produto);

        estoqueRepository.save(new Estoque(produto, dados.quantidadeInicial()));

        log.info("Produto cadastrado id={} nome='{}'", produto.getIdProduto(), nome);
        return new DTODetalhamentoProduto(produto);
    }

    /** Cardapio visivel ao cliente: so ativos, com preco e saldo. */
    @Transactional(readOnly = true)
    public List<DTOListagemProduto> listarDisponiveis() {
        List<Produto> ativos = produtoRepository.findAllByProdutoAtivoTrueOrderByNomeProdutoAsc();
        Map<Long, Integer> saldos = mapearSaldos();

        return ativos.stream()
                .map(produto -> DTOListagemProduto.de(produto, saldos.get(produto.getIdProduto())))
                .toList();
    }

    /** Cardapio completo (inclusive inativos) para o painel do funcionario. */
    @Transactional(readOnly = true)
    public List<DTOListagemProduto> listarTodos() {
        Map<Long, Integer> saldos = mapearSaldos();
        return produtoRepository.findAll().stream()
                .map(produto -> DTOListagemProduto.de(produto, saldos.get(produto.getIdProduto())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DTODetalhamentoProduto buscarPorId(Long idProduto) {
        return new DTODetalhamentoProduto(buscarEntidade(idProduto));
    }

    @Transactional(readOnly = true)
    public DTODetalhamentoProduto buscarPorNome(String nomeProduto) {
        Produto produto = produtoRepository.findByNomeProduto(nomeProduto)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(
                        "Produto nao encontrado pelo nome: " + nomeProduto));
        return new DTODetalhamentoProduto(produto);
    }

    /**
     * Atualizacao parcial: cada campo so muda se veio preenchido.
     *
     * A versao anterior tinha um setNomeProduto solto depois dos ifs, entao um
     * PUT sem nome apagava o nome do produto mesmo com a guarda de nulo logo
     * acima.
     */
    @Transactional
    public DTODetalhamentoProduto atualizar(Long idProduto, DTOAtualizarProduto dados) {
        Produto produto = buscarEntidade(idProduto);

        if (dados.nomeProduto() != null && !dados.nomeProduto().isBlank()) {
            String novoNome = dados.nomeProduto().trim();
            if (produtoRepository.existsByNomeProdutoAndIdProdutoNot(novoNome, idProduto)) {
                throw new RecursoDuplicadoException("Ja existe um produto chamado '" + novoNome + "'");
            }
            produto.setNomeProduto(novoNome);
        }
        if (dados.descricaoProduto() != null) {
            produto.setDescricaoProduto(dados.descricaoProduto());
        }
        if (dados.preco() != null) {
            produto.setPreco(dados.preco());
        }
        if (dados.categoria() != null) {
            produto.setCategoria(dados.categoria());
        }
        if (dados.produtoAtivo() != null) {
            produto.setProdutoAtivo(dados.produtoAtivo());
        }
        return new DTODetalhamentoProduto(produto);
    }

    /**
     * Tira do cardapio sem apagar a linha: itens de pedidos antigos referenciam
     * o produto, e um DELETE derrubaria a chave estrangeira.
     */
    @Transactional
    public void desativar(Long idProduto) {
        Produto produto = buscarEntidade(idProduto);
        produto.setProdutoAtivo(false);
        log.info("Produto id={} desativado", idProduto);
    }

    @Transactional
    public void reativar(Long idProduto) {
        Produto produto = buscarEntidade(idProduto);
        produto.setProdutoAtivo(true);
        log.info("Produto id={} reativado", idProduto);
    }

    @Transactional(readOnly = true)
    public Produto buscarEntidade(Long idProduto) {
        return produtoRepository.findById(idProduto)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(
                        "Produto nao encontrado. ID: " + idProduto));
    }

    /** Uma consulta so, em vez de um SELECT de estoque por produto listado. */
    private Map<Long, Integer> mapearSaldos() {
        return estoqueRepository.buscarTodosComProduto().stream()
                .collect(Collectors.toMap(
                        estoque -> estoque.getProduto().getIdProduto(),
                        Estoque::getQuantidade,
                        (a, b) -> a
                ));
    }
}
