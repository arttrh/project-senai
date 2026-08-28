package br.com.cantina.senai.exception;

public class PedidoNaoEncontradoException extends RecursoNaoEncontradoException {
    public PedidoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
