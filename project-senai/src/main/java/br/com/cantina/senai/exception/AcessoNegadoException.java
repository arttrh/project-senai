package br.com.cantina.senai.exception;

/** Recurso existe, mas nao pertence a quem pediu. Vira 403. */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
