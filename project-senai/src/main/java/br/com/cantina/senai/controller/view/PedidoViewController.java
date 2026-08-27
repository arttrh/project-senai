package br.com.cantina.senai.controller.view;

import br.com.cantina.senai.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Revisao e fechamento do carrinho. */
@Controller
public class PedidoViewController {

    @GetMapping("/pedido/finalizar")
    public String exibirFinalizar(@AuthenticationPrincipal UsuarioAutenticado autenticado, Model model) {
        model.addAttribute("nomeUsuario", autenticado.getNome());
        return "finalizarPedido";
    }

    @GetMapping("/pedido/meus")
    public String exibirMeusPedidos(@AuthenticationPrincipal UsuarioAutenticado autenticado, Model model) {
        model.addAttribute("nomeUsuario", autenticado.getNome());
        return "meusPedidos";
    }
}
