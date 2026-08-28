package br.com.cantina.senai.controller.view;

import br.com.cantina.senai.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Telas publicas e cardapio. */
@Controller
public class HomeController {

    /** A raiz nao tinha mapeamento nenhum e respondia 404. */
    @GetMapping("/")
    public String raiz(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        if (autenticado == null) {
            return "redirect:/login";
        }
        return autenticado.getUsuario().isFuncionario() ? "redirect:/funcionario" : "redirect:/home";
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return autenticado == null ? "login" : "redirect:/";
    }

    /**
     * O cardapio e renderizado pelo JS a partir de /api/produtos, que ja traz
     * preco e saldo. O controller so entrega a pagina e o nome de quem entrou.
     */
    @GetMapping("/home")
    public String exibirHome(@AuthenticationPrincipal UsuarioAutenticado autenticado, Model model) {
        model.addAttribute("nomeUsuario", autenticado.getNome());
        return "home";
    }
}
