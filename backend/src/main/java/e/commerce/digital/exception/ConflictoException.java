package e.commerce.digital.exception;

// La operación choca con el estado actual de los datos: duplicados, stock, estados (responder 409)
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
