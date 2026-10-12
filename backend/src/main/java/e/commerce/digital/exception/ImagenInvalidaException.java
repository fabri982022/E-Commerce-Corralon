package e.commerce.digital.exception;

// Archivo vacío, demasiado grande o de un tipo no permitido (responder 400)
public class ImagenInvalidaException extends RuntimeException {

    public ImagenInvalidaException(String mensaje) {
        super(mensaje);
    }
}
