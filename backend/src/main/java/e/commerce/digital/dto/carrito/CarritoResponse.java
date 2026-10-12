package e.commerce.digital.dto.carrito;

import e.commerce.digital.models.Carrito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CarritoResponse(
        Long idCarrito,
        String estado,
        LocalDateTime fechaCreacion,
        List<CarritoItemResponse> items,
        BigDecimal total
) {
    public static CarritoResponse desde(Carrito c) {
        return new CarritoResponse(
                c.getIdCarrito(),
                c.getEstado(),
                c.getFechaCreacion(),
                c.getItems().stream().map(CarritoItemResponse::desde).toList(),
                c.getTotal());
    }
}
