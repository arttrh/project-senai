package br.com.cantina.senai.exception;

/** Pedido maior que o saldo disponivel. */
public class EstoqueInsuficienteException extends RegraDeNegocioException {

    public EstoqueInsuficienteException(String nomeProduto, int solicitado, int disponivel) {
        super("Estoque insuficiente para '" + nomeProduto + "': pedido " + solicitado
                + ", disponivel " + disponivel);
    }
}
