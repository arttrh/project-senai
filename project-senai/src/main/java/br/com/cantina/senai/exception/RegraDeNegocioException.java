package br.com.cantina.senai.exception;

/**
 * Base de tudo que vira 409/422: a requisicao esta bem formada, mas fere uma
 * regra do dominio.
 */
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
