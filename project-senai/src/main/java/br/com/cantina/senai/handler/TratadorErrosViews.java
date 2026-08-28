package br.com.cantina.senai.handler;

import br.com.cantina.senai.exception.AcessoNegadoException;
import br.com.cantina.senai.exception.RecursoNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Erro nas telas Thymeleaf devolve pagina, nao JSON. Fica separado do
 * tratador da API justamente para cada um responder no formato certo.
 */
@ControllerAdvice(basePackages = "br.com.cantina.senai.controller.view")
public class TratadorErrosViews {

    private static final Logger log = LoggerFactory.getLogger(TratadorErrosViews.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String tratarNaoEncontrado(RecursoNaoEncontradoException e, Model model) {
        model.addAttribute("titulo", "Pagina nao encontrada");
        model.addAttribute("mensagem", e.getMessage());
        return "erro";
    }

    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String tratarAcessoNegado(Model model) {
        model.addAttribute("titulo", "Acesso negado");
        model.addAttribute("mensagem", "Voce nao tem permissao para acessar esta area.");
        return "erro";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String tratarInesperado(Exception e, Model model) {
        log.error("Erro nao tratado ao renderizar uma tela", e);
        model.addAttribute("titulo", "Algo deu errado");
        model.addAttribute("mensagem", "Tente novamente em instantes.");
        return "erro";
    }
}
