package br.com.cantina.senai.exception;

public class ProdutoNaoEncontradoException extends RecursoNaoEncontradoException {
    public ProdutoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
