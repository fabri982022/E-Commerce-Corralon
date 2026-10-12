package e.commerce.digital.mappers;

import e.commerce.digital.dto.comprobante.ComprobanteResponse;
import e.commerce.digital.models.Comprobante;
import e.commerce.digital.models.Pago;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ComprobanteMapper {

    // No hay request: el servidor genera el comprobante a partir del pago.
    // El número lo genera el service; para asociarlo usar pago.setComprobante(comprobante).
    public Comprobante toEntity(Pago pago, String numero, String tipo, String url) {
        Comprobante c = new Comprobante();
        c.setNumero(numero);
        c.setTipo(tipo);
        c.setFechaEmision(LocalDateTime.now());
        c.setImporte(pago.getImporte());
        c.setUrl(url);
        return c;
    }

    public ComprobanteResponse toResponse(Comprobante comprobante) {
        return ComprobanteResponse.desde(comprobante);
    }
}