package br.com.cantina.senai.exception;

public class UsuarioNaoEncontradoException extends RecursoNaoEncontradoException {
    public UsuarioNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
