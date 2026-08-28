package br.com.cantina.senai.service;

import br.com.cantina.senai.dto.estoque.*;
import br.com.cantina.senai.exception.EstoqueNaoEncontradoException;
import br.com.cantina.senai.exception.RecursoDuplicadoException;
import br.com.cantina.senai.model.estoque.Estoque;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.repository.EstoqueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Saldo dos produtos. A baixa por venda fica no PedidoService. */
@Service
public class EstoqueService {

    private static final Logger log = LoggerFactory.getLogger(EstoqueService.class);

    private final EstoqueRepository estoqueRepository;
    private final ProdutoService produtoService;

    public EstoqueService(EstoqueRepository estoqueRepository, ProdutoService produtoService) {
        this.estoqueRepository = estoqueRepository;
        this.produtoService = produtoService;
    }

    /**
     * Abre a linha de estoque de um produto que ainda nao tem uma.
     *
     * A versao anterior permitia varias linhas para o mesmo produto, e ai
     * findByProduto_IdProduto quebrava com NonUniqueResultException na hora da
     * venda. Agora o banco tem UNIQUE e o service recusa antes.
     */
    @Transactional
    public DTODetalhamentoEstoque cadastrar(DTOCadastroEstoque dados) {
        Produto produto = produtoService.buscarEntidade(dados.idProduto());

        if (estoqueRepository.existsByProduto_IdProduto(produto.getIdProduto())) {
            throw new RecursoDuplicadoException(
                    "O produto '" + produto.getNomeProduto() + "' ja possui estoque. Use a atualizacao.");
        }

        Estoque estoque = estoqueRepository.save(new Estoque(produto, dados.quantidade()));
        log.info("Estoque criado id={} produto={} quantidade={}",
                estoque.getIdEstoque(), produto.getIdProduto(), dados.quantidade());
        return new DTODetalhamentoEstoque(estoque);
    }

    @Transactional(readOnly = true)
    public List<DTOListagemEstoque> listarTodos() {
        return estoqueRepository.buscarTodosComProduto().stream()
                .map(DTOListagemEstoque::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DTODetalhamentoEstoque buscarPorId(Long idEstoque) {
        return new DTODetalhamentoEstoque(buscarEntidade(idEstoque));
    }

    @Transactional(readOnly = true)
    public DTODetalhamentoEstoque buscarPorProduto(Long idProduto) {
        Estoque estoque = estoqueRepository.findByProduto_IdProduto(idProduto)
                .orElseThrow(() -> new EstoqueNaoEncontradoException(
                        "Nao ha estoque para o produto ID: " + idProduto));
        return new DTODetalhamentoEstoque(estoque);
    }

    /** Ajuste manual do saldo pelo funcionario (recontagem, reposicao). */
    @Transactional
    public DTODetalhamentoEstoque atualizar(Long idEstoque, DTOAtualizarEstoque dados) {
        Estoque estoque = buscarEntidade(idEstoque);
        int anterior = estoque.getQuantidade();
        estoque.setQuantidade(dados.quantidade());

        log.info("Estoque id={} ajustado de {} para {}", idEstoque, anterior, dados.quantidade());
        return new DTODetalhamentoEstoque(estoque);
    }

    @Transactional
    public void excluir(Long idEstoque) {
        Estoque estoque = buscarEntidade(idEstoque);
        estoqueRepository.delete(estoque);
        log.info("Estoque id={} removido", idEstoque);
    }

    @Transactional(readOnly = true)
    public Estoque buscarEntidade(Long idEstoque) {
        return estoqueRepository.buscarPorIdComProduto(idEstoque)
                .orElseThrow(() -> new EstoqueNaoEncontradoException(
                        "Estoque nao encontrado. ID: " + idEstoque));
    }
}
