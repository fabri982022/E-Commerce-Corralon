package e.commerce.digital.exception;

import e.commerce.digital.util.enumerated.EstadoPedido;

public class TransicionInvalidaException extends ConflictoException {

    public TransicionInvalidaException(EstadoPedido actual, EstadoPedido nuevo) {
        super("No se puede cambiar el pedido de " + actual + " a " + nuevo);
    }
}
