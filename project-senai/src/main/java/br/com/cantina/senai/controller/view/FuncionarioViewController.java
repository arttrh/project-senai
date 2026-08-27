package br.com.cantina.senai.controller.view;

import br.com.cantina.senai.security.UsuarioAutenticado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Painel da cantina. Conteudo carregado pelo funcionario.js via API. */
@Controller
@PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
public class FuncionarioViewController {

    @GetMapping("/funcionario")
    public String exibirPainel(@AuthenticationPrincipal UsuarioAutenticado autenticado, Model model) {
        model.addAttribute("nomeUsuario", autenticado.getNome());
        return "funcionario";
    }
}
