package e.commerce.digital.exception;

// El registro no existe, o existe pero pertenece a otro usuario (responder 404)
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
