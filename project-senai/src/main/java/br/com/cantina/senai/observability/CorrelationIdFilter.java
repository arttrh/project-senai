package br.com.cantina.senai.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Marca cada requisicao com um id que aparece em toda linha de log dela.
 *
 * Sem isso, investigar "o pedido do fulano deu erro" significa ler logs
 * intercalados de varios usuarios sem saber quais linhas sao da mesma chamada.
 * O id tambem volta no header para o cliente poder citar na hora de reportar.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String MDC_USUARIO = "usuario";

    private static final int TAMANHO_MAXIMO = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao,
                                    HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        String correlationId = sanitizar(requisicao.getHeader(HEADER));

        MDC.put(MDC_KEY, correlationId);
        if (requisicao.getUserPrincipal() != null) {
            MDC.put(MDC_USUARIO, requisicao.getUserPrincipal().getName());
        }
        resposta.setHeader(HEADER, correlationId);

        try {
            cadeia.doFilter(requisicao, resposta);
        } finally {
            // Threads sao reaproveitadas pelo container: sem o clear, a proxima
            // requisicao herdaria o id da anterior.
            MDC.remove(MDC_KEY);
            MDC.remove(MDC_USUARIO);
        }
    }

    /**
     * Nao confia no header do cliente: valor estranho vira id novo, para nao
     * injetarem quebra de linha ou lixo no arquivo de log.
     */
    private String sanitizar(String recebido) {
        if (recebido == null || recebido.isBlank() || recebido.length() > TAMANHO_MAXIMO
                || !recebido.matches("[A-Za-z0-9._-]+")) {
            return UUID.randomUUID().toString();
        }
        return recebido;
    }
}
