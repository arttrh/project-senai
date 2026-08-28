package br.com.cantina.senai.exception;

/** Viola uma chave unica de negocio (e-mail, CPF, nome de produto). */
public class RecursoDuplicadoException extends RegraDeNegocioException {
    public RecursoDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
