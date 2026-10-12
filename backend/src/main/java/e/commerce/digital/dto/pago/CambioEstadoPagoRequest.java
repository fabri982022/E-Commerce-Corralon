package e.commerce.digital.dto.pago;

import e.commerce.digital.util.enumerated.EstadoPago;
import jakarta.validation.constraints.NotNull;

// Uso exclusivo del administrador (o del proveedor de pagos) para aprobar, rechazar o reembolsar
public record CambioEstadoPagoRequest(
        @NotNull EstadoPago estado
) {
}
