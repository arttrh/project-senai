package br.com.cantina.senai.controller.api;

import br.com.cantina.senai.dto.produto.*;
import br.com.cantina.senai.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Cardapio.
 *
 * O POST anterior mapeado aqui se chamava "criarPedido", nao usava o corpo
 * recebido, nao gravava nada e mesmo assim devolvia 201 Created: o cliente era
 * informado de um sucesso que nunca aconteceu.
 */
@RestController
@RequestMapping("/api/produtos")
public class ProdutoApiController {

    private final ProdutoService produtoService;

    public ProdutoApiController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    /** Cardapio do cliente: apenas produtos ativos, com preco e saldo. */
    @GetMapping
    public List<DTOListagemProduto> listar() {
        return produtoService.listarDisponiveis();
    }

    /** Cardapio completo, incluindo itens fora de linha (painel da cantina). */
    @GetMapping("/todos")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
    public List<DTOListagemProduto> listarTodos() {
        return produtoService.listarTodos();
    }

    @GetMapping("/{id}")
    public DTODetalhamentoProduto buscarPorId(@PathVariable Long id) {
        return produtoService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
    public ResponseEntity<DTODetalhamentoProduto> cadastrar(@RequestBody @Valid DTOCadastroProduto dados,
                                                            UriComponentsBuilder uriBuilder) {
        DTODetalhamentoProduto criado = produtoService.cadastrar(dados);
        var uri = uriBuilder.path("/api/produtos/{id}").buildAndExpand(criado.idProduto()).toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
    public DTODetalhamentoProduto atualizar(@PathVariable Long id,
                                            @RequestBody @Valid DTOAtualizarProduto dados) {
        return produtoService.atualizar(id, dados);
    }

    /** Tira do cardapio sem apagar: pedidos antigos ainda apontam para o item. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        produtoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
    public ResponseEntity<Void> reativar(@PathVariable Long id) {
        produtoService.reativar(id);
        return ResponseEntity.noContent().build();
    }
}
