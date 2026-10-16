package e.commerce.digital.exception;

public class ProductoNoDisponibleException extends ConflictoException {

    public ProductoNoDisponibleException(String producto) {
        super("El producto no está disponible: " + producto);
    }
}
