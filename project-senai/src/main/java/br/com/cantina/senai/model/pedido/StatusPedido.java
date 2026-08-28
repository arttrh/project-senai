package br.com.cantina.senai.model.pedido;

import java.util.Set;

/**
 * Ciclo de vida do pedido. As transicoes validas ficam aqui para que a regra
 * viva em um lugar so, em vez de espalhada pelos controllers.
 */
public enum StatusPedido {
    CRIADO,
    EM_PREPARACAO,
    PRONTO,
    FINALIZADO,
    CANCELADO;

    public Set<StatusPedido> proximosPermitidos() {
        return switch (this) {
            case CRIADO -> Set.of(EM_PREPARACAO, CANCELADO);
            case EM_PREPARACAO -> Set.of(PRONTO, CANCELADO);
            case PRONTO -> Set.of(FINALIZADO, CANCELADO);
            case FINALIZADO, CANCELADO -> Set.of();
        };
    }

    public boolean podeTransicionarPara(StatusPedido destino) {
        return proximosPermitidos().contains(destino);
    }

    /** Pedido encerrado nao devolve estoque nem aceita alteracao. */
    public boolean isFinal() {
        return this == FINALIZADO || this == CANCELADO;
    }
}
