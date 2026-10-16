package e.commerce.digital.exception;

// La solicitud es válida en formato pero no se puede realizar: carrito vacío, importe excedido (responder 400)
public class OperacionInvalidaException extends RuntimeException {

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
