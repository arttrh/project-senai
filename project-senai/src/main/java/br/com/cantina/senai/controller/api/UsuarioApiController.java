package br.com.cantina.senai.controller.api;

import br.com.cantina.senai.dto.usuario.*;
import br.com.cantina.senai.security.UsuarioAutenticado;
import br.com.cantina.senai.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * API de contas.
 *
 * A classe anterior tinha este nome e estava completamente vazia, sem sequer
 * @RestController, enquanto o Cadastro.js chamava /api/usuarios/cadastrar: o
 * cadastro pela tela respondia 404 e nunca funcionou.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioApiController {

    private final UsuarioService usuarioService;

    public UsuarioApiController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Auto-cadastro. Unico endpoint publico: sempre cria como USUARIO. */
    @PostMapping
    public ResponseEntity<DTODetalhamentoUsuario> cadastrar(@RequestBody @Valid DTOCadastroUsuario dados,
                                                            UriComponentsBuilder uriBuilder) {
        DTODetalhamentoUsuario criado = usuarioService.cadastrar(dados);
        var uri = uriBuilder.path("/api/usuarios/{id}").buildAndExpand(criado.idUsuario()).toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<DTOListagemUsuario> listar() {
        return usuarioService.listar();
    }

    /** Dados da sessao atual, usados pelas telas para mostrar o nome. */
    @GetMapping("/eu")
    public DTODetalhamentoUsuario meuPerfil(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return usuarioService.buscarPorId(autenticado.getIdUsuario());
    }

    @PutMapping("/eu")
    public DTODetalhamentoUsuario atualizarMeuPerfil(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @RequestBody @Valid DTOAtualizarUsuario dados) {
        return usuarioService.atualizar(autenticado.getIdUsuario(), dados);
    }

    @PutMapping("/eu/senha")
    public ResponseEntity<Void> alterarMinhaSenha(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @RequestBody @Valid DTOAlterarSenha dados) {
        usuarioService.alterarSenha(autenticado.getIdUsuario(), dados);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.idUsuario")
    public DTODetalhamentoUsuario buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }

    /**
     * Troca de perfil, so para ADMIN.
     *
     * Antes o tipo vinha junto do PUT de perfil, entao qualquer usuario logado
     * se promovia a ADMIN editando o proprio cadastro.
     */
    @PutMapping("/{id}/tipo")
    @PreAuthorize("hasRole('ADMIN')")
    public DTODetalhamentoUsuario alterarTipo(@PathVariable Long id,
                                              @RequestBody @Valid DTOAlterarTipoUsuario dados) {
        return usuarioService.alterarTipo(id, dados.tipoUsuario());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        usuarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
