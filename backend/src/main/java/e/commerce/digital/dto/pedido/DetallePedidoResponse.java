package e.commerce.digital.dto.pedido;

import e.commerce.digital.models.DetallePedido;

import java.math.BigDecimal;

public record DetallePedidoResponse(
        Long idDetalle,
        Long idProducto,
        String nombreProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static DetallePedidoResponse desde(DetallePedido d) {
        return new DetallePedidoResponse(
                d.getIdDetalle(),
                d.getProducto().getIdProducto(),
                d.getProducto().getNombre(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getSubtotal());
    }
}
