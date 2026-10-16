package e.commerce.digital.exception;

public class StockInsuficienteException extends ConflictoException {

    public StockInsuficienteException(String producto) {
        super("Stock insuficiente para el producto: " + producto);
    }
}
