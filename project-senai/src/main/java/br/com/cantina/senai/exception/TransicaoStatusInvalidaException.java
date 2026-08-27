package br.com.cantina.senai.exception;

import br.com.cantina.senai.model.pedido.StatusPedido;

/** Tentativa de pular ou reverter etapas do ciclo de vida do pedido. */
public class TransicaoStatusInvalidaException extends RegraDeNegocioException {

    public TransicaoStatusInvalidaException(StatusPedido atual, StatusPedido destino) {
        super("Nao e possivel mudar o pedido de " + atual + " para " + destino
                + ". Transicoes validas: " + atual.proximosPermitidos());
    }
}
