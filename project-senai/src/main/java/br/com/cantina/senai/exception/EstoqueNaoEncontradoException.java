package br.com.cantina.senai.exception;

public class EstoqueNaoEncontradoException extends RecursoNaoEncontradoException {
    public EstoqueNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
