package e.commerce.digital.dto.comprobante;

import e.commerce.digital.models.Comprobante;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Sin request: el comprobante lo genera el servidor al aprobarse un pago
public record ComprobanteResponse(
        Long idComprobante,
        String numero,
        String tipo,
        LocalDateTime fechaEmision,
        BigDecimal importe,
        String url,
        Long idPago
) {
    public static ComprobanteResponse desde(Comprobante c) {
        return new ComprobanteResponse(
                c.getIdComprobante(),
                c.getNumero(),
                c.getTipo(),
                c.getFechaEmision(),
                c.getImporte(),
                c.getUrl(),
                c.getPago().getIdPago());
    }
}
