package e.commerce.digital.dto.pago;

import e.commerce.digital.dto.comprobante.ComprobanteResponse;
import e.commerce.digital.models.Pago;
import e.commerce.digital.util.enumerated.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(
        Long idPago,
        String metodo,
        BigDecimal importe,
        LocalDateTime fechaPago,
        EstadoPago estado,
        String referenciaOperacion,
        Long idPedido,
        ComprobanteResponse comprobante
) {
    public static PagoResponse desde(Pago p) {
        return new PagoResponse(
                p.getIdPago(),
                p.getMetodo(),
                p.getImporte(),
                p.getFechaPago(),
                p.getEstado(),
                p.getReferenciaOperacion(),
                p.getPedido().getIdPedido(),
                p.getComprobante() == null ? null : ComprobanteResponse.desde(p.getComprobante()));
    }
}
