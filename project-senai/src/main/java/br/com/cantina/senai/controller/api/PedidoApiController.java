package br.com.cantina.senai.controller.api;

import br.com.cantina.senai.dto.pedido.DTOCadastroPedido;
import br.com.cantina.senai.dto.pedido.DTODetalhamentoPedido;
import br.com.cantina.senai.dto.pedido.DTOListagemPedido;
import br.com.cantina.senai.security.UsuarioAutenticado;
import br.com.cantina.senai.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Pedidos do cliente.
 *
 * O dono do pedido vem da sessao. Antes o metodo era
 * {@code pedidoService.criarPedido(dados, 1L)}: todo pedido do sistema era
 * gravado no nome do usuario de id 1, quem quer que ele fosse.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoApiController {

    private final PedidoService pedidoService;

    public PedidoApiController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<DTODetalhamentoPedido> criar(
            @RequestBody @Valid DTOCadastroPedido dados,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            UriComponentsBuilder uriBuilder) {

        DTODetalhamentoPedido criado = pedidoService.criar(dados, autenticado.getIdUsuario());
        var uri = uriBuilder.path("/api/pedidos/{id}").buildAndExpand(criado.idPedido()).toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @GetMapping("/meus")
    public List<DTOListagemPedido> meusPedidos(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return pedidoService.listarDoUsuario(autenticado.getIdUsuario());
    }

    /** O service recusa se o pedido for de outra pessoa. */
    @GetMapping("/{id}")
    public DTODetalhamentoPedido buscarPorId(@PathVariable Long id,
                                             @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return pedidoService.buscarParaUsuario(id, autenticado.getUsuario());
    }

    @PostMapping("/{id}/cancelar")
    public DTODetalhamentoPedido cancelar(@PathVariable Long id,
                                          @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return pedidoService.cancelarComoCliente(id, autenticado.getUsuario());
    }
}
