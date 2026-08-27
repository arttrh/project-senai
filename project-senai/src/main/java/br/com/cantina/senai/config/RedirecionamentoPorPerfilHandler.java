package br.com.cantina.senai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Depois do login, funcionario e admin caem no painel de operacao e o cliente
 * no cardapio, em vez de todo mundo ir para a mesma tela.
 */
public class RedirecionamentoPorPerfilHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Set<String> perfis = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        boolean operacao = perfis.contains("ROLE_FUNCIONARIO") || perfis.contains("ROLE_ADMIN");
        getRedirectStrategy().sendRedirect(request, response, operacao ? "/funcionario" : "/home");
    }
}
