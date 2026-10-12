package e.commerce.digital.dto.carrito;

import e.commerce.digital.models.CarritoItem;

import java.math.BigDecimal;

public record CarritoItemResponse(
        Long idCarritoItem,
        Long idProducto,
        String nombreProducto,
        String imagenUrl,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static CarritoItemResponse desde(CarritoItem i) {
        return new CarritoItemResponse(
                i.getIdCarritoItem(),
                i.getProducto().getIdProducto(),
                i.getProducto().getNombre(),
                i.getProducto().getImagenUrl(),
                i.getCantidad(),
                i.getPrecioUnitario(),
                i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())));
    }
}
