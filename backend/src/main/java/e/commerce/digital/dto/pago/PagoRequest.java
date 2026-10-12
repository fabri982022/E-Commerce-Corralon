package e.commerce.digital.dto.pago;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Registro de un pago sobre un pedido; el estado inicial lo fija el servidor (PENDIENTE)
public record PagoRequest(
        @NotBlank @Size(max = 30) String metodo,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal importe,
        @Size(max = 100) String referenciaOperacion
) {
}
