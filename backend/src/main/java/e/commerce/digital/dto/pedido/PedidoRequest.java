package e.commerce.digital.dto.pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Confirmar el carrito del usuario autenticado; items, precios y total salen del servidor
public record PedidoRequest(
        @NotBlank @Size(max = 255) String direccionEntrega
) {
}
