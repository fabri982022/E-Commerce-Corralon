package e.commerce.digital.dto.carrito;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Agregar un producto al carrito
public record CarritoItemRequest(
        @NotNull Long idProducto,
        @NotNull @Positive Integer cantidad
) {
}
