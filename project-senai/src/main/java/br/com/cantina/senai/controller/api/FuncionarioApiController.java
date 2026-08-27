package br.com.cantina.senai.controller.api;

import br.com.cantina.senai.dto.estoque.DTOAtualizarEstoque;
import br.com.cantina.senai.dto.estoque.DTODetalhamentoEstoque;
import br.com.cantina.senai.dto.estoque.DTOListagemEstoque;
import br.com.cantina.senai.dto.pedido.DTOAtualizarStatusPedido;
import br.com.cantina.senai.dto.pedido.DTODetalhamentoPedido;
import br.com.cantina.senai.dto.produto.DTOCadastroProduto;
import br.com.cantina.senai.dto.produto.DTODetalhamentoProduto;
import br.com.cantina.senai.dto.produto.DTOListagemProduto;
import br.com.cantina.senai.service.EstoqueService;
import br.com.cantina.senai.service.PedidoService;
import br.com.cantina.senai.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Painel de operacao da cantina.
 *
 * Duas correcoes centrais em relacao a versao anterior:
 *  - a listagem devolvia List<Pedido>, a propria entidade JPA, o que expunha o
 *    modelo interno e quebrava na serializacao das associacoes lazy;
 *  - o funcionario.js chamava PATCH /pedidos/{id}/status, um endpoint que nunca
 *    existiu, entao os botoes "marcar como pronto" e "confirmar entrega"
 *    respondiam 404 e nada acontecia na tela.
 */
@RestController
@RequestMapping("/api/funcionario")
@PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
public class FuncionarioApiController {

    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;
    private final PedidoService pedidoService;

    public FuncionarioApiController(ProdutoService produtoService,
                                    EstoqueService estoqueService,
                                    PedidoService pedidoService) {
        this.produtoService = produtoService;
        this.estoqueService = estoqueService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/pedidos")
    public List<DTODetalhamentoPedido> listarPedidosEmAndamento() {
        return pedidoService.listarEmAndamento();
    }

    @GetMapping("/pedidos/{id}")
    public DTODetalhamentoPedido buscarPedido(@PathVariable Long id) {
        return pedidoService.buscarPorId(id);
    }

    /** Avanca o pedido na fila. O service recusa transicoes invalidas. */
    @PatchMapping("/pedidos/{id}/status")
    public DTODetalhamentoPedido alterarStatus(@PathVariable Long id,
                                               @RequestBody @Valid DTOAtualizarStatusPedido dados) {
        return pedidoService.alterarStatus(id, dados.status());
    }

    @GetMapping("/estoque")
    public List<DTOListagemEstoque> listarEstoque() {
        return estoqueService.listarTodos();
    }

    @PutMapping("/estoque/{id}")
    public DTODetalhamentoEstoque atualizarEstoque(@PathVariable Long id,
                                                   @RequestBody @Valid DTOAtualizarEstoque dados) {
        return estoqueService.atualizar(id, dados);
    }

    @GetMapping("/produtos")
    public List<DTOListagemProduto> listarProdutos() {
        return produtoService.listarTodos();
    }

    @PostMapping("/produtos")
    public ResponseEntity<DTODetalhamentoProduto> cadastrarProduto(
            @RequestBody @Valid DTOCadastroProduto dados) {
        return ResponseEntity.status(201).body(produtoService.cadastrar(dados));
    }
}
