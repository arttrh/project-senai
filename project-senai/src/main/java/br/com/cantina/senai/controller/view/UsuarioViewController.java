package br.com.cantina.senai.controller.view;

import br.com.cantina.senai.dto.usuario.DTOAlterarSenha;
import br.com.cantina.senai.dto.usuario.DTOAtualizarUsuario;
import br.com.cantina.senai.dto.usuario.DTOCadastroUsuario;
import br.com.cantina.senai.exception.RegraDeNegocioException;
import br.com.cantina.senai.security.UsuarioAutenticado;
import br.com.cantina.senai.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas de conta: cadastro e configuracoes.
 *
 * O nome da classe anterior era "UsuarioVIewController" (com o I maiusculo no
 * meio), o POST de cadastro nao tinha @Valid nem BindingResult, e o formulario
 * do cadastro.html apontava para POST /home, rota que nao existia: o cadastro
 * pela tela terminava em 405.
 */
@Controller
public class UsuarioViewController {

    private final UsuarioService usuarioService;

    public UsuarioViewController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuario/cadastrar")
    public String exibirCadastro(Model model) {
        if (!model.containsAttribute("dados")) {
            model.addAttribute("dados", new DTOCadastroUsuario("", "", "", "", ""));
        }
        return "cadastro";
    }

    @PostMapping("/usuario/cadastrar")
    public String processarCadastro(@ModelAttribute("dados") @Valid DTOCadastroUsuario dados,
                                    BindingResult resultado,
                                    Model model) {
        if (resultado.hasErrors()) {
            model.addAttribute("erro", "Confira os campos destacados.");
            return "cadastro";
        }
        try {
            usuarioService.cadastrar(dados);
        } catch (RegraDeNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "cadastro";
        }
        return "redirect:/login?cadastrado";
    }

    @GetMapping("/configuracoes")
    public String exibirConfiguracoes(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                      Model model) {
        model.addAttribute("usuario", usuarioService.buscarPorId(autenticado.getIdUsuario()));
        return "configuracaoUsuario";
    }

    @PostMapping("/configuracoes/perfil")
    public String atualizarPerfil(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                  @Valid @ModelAttribute DTOAtualizarUsuario dados,
                                  BindingResult resultado,
                                  RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            atributos.addFlashAttribute("erro", "Confira os campos informados.");
            return "redirect:/configuracoes";
        }
        try {
            usuarioService.atualizar(autenticado.getIdUsuario(), dados);
            atributos.addFlashAttribute("sucesso", "Perfil atualizado com sucesso.");
        } catch (RegraDeNegocioException e) {
            atributos.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/configuracoes";
    }

    @PostMapping("/configuracoes/senha")
    public String alterarSenha(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                               @Valid @ModelAttribute DTOAlterarSenha dados,
                               BindingResult resultado,
                               RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            atributos.addFlashAttribute("erro", "A nova senha precisa de no minimo 8 caracteres.");
            return "redirect:/configuracoes";
        }
        try {
            usuarioService.alterarSenha(autenticado.getIdUsuario(), dados);
            atributos.addFlashAttribute("sucesso", "Senha alterada com sucesso.");
        } catch (RegraDeNegocioException e) {
            atributos.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/configuracoes";
    }
}
