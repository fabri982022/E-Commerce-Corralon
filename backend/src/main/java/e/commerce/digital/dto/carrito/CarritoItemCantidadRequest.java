package e.commerce.digital.dto.carrito;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Cambiar la cantidad de un item existente
public record CarritoItemCantidadRequest(
        @NotNull @Positive Integer cantidad
) {
}
