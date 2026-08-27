package br.com.cantina.senai.controller.api;

import br.com.cantina.senai.dto.estoque.*;
import br.com.cantina.senai.service.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Estoque. Todo o controller exige FUNCIONARIO ou ADMIN: antes estava aberto,
 * e qualquer pessoa podia apagar uma linha de estoque com um DELETE.
 */
@RestController
@RequestMapping("/api/estoque")
@PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
public class EstoqueApiController {

    private final EstoqueService estoqueService;

    public EstoqueApiController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping
    public List<DTOListagemEstoque> listarTodos() {
        return estoqueService.listarTodos();
    }

    @GetMapping("/{id}")
    public DTODetalhamentoEstoque buscarPorId(@PathVariable Long id) {
        return estoqueService.buscarPorId(id);
    }

    @GetMapping("/produto/{idProduto}")
    public DTODetalhamentoEstoque buscarPorProduto(@PathVariable Long idProduto) {
        return estoqueService.buscarPorProduto(idProduto);
    }

    @PostMapping
    public ResponseEntity<DTODetalhamentoEstoque> cadastrar(@RequestBody @Valid DTOCadastroEstoque dados,
                                                            UriComponentsBuilder uriBuilder) {
        DTODetalhamentoEstoque criado = estoqueService.cadastrar(dados);
        var uri = uriBuilder.path("/api/estoque/{id}").buildAndExpand(criado.idEstoque()).toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @PutMapping("/{id}")
    public DTODetalhamentoEstoque atualizar(@PathVariable Long id,
                                            @RequestBody @Valid DTOAtualizarEstoque dados) {
        return estoqueService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        estoqueService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
