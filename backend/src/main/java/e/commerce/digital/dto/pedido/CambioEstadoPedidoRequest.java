package e.commerce.digital.dto.pedido;

import e.commerce.digital.util.enumerated.EstadoPedido;
import jakarta.validation.constraints.NotNull;

// Uso exclusivo del administrador; el service valida que la transición sea permitida
public record CambioEstadoPedidoRequest(
        @NotNull EstadoPedido estado
) {
}
