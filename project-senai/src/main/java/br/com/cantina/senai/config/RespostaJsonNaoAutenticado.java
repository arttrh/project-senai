package br.com.cantina.senai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Chamada de API sem sessao recebe 401 em JSON, nao um redirect 302 para a
 * pagina de login: o fetch do front nao sabe seguir isso e mostraria o HTML
 * do login como se fosse a resposta da API.
 */
public class RespostaJsonNaoAutenticado implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException excecao) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"status":401,"erro":"Nao autenticado","mensagem":"Faca login para continuar"}""");
    }
}
