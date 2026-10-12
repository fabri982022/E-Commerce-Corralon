package e.commerce.digital.mapper;

import e.commerce.digital.dto.pago.PagoRequest;
import e.commerce.digital.dto.pago.PagoResponse;
import e.commerce.digital.models.Pago;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    // Estado PENDIENTE por defecto; el pedido se asocia con pedido.agregarPago(pago) en el service
    public Pago toEntity(PagoRequest req) {
        Pago pago = new Pago();
        pago.setMetodo(req.metodo());
        pago.setImporte(req.importe());
        pago.setReferenciaOperacion(req.referenciaOperacion());
        return pago;
    }

    public PagoResponse toResponse(Pago pago) {
        return PagoResponse.desde(pago);
    }
}
