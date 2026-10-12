package e.commerce.digital.exception;

// Falla al comunicarse con Cloudinary (responder 502 o 503)
public class ServicioImagenesException extends RuntimeException {

    public ServicioImagenesException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
