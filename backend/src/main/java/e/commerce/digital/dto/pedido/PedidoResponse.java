package e.commerce.digital.dto.pedido;

import e.commerce.digital.dto.pago.PagoResponse;
import e.commerce.digital.models.Pedido;
import e.commerce.digital.util.enumerated.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long idPedido,
        LocalDateTime fechaPedido,
        EstadoPedido estado,
        BigDecimal total,
        String direccionEntrega,
        Long idUsuario,
        String nombreCliente,
        List<DetallePedidoResponse> detalles,
        List<PagoResponse> pagos
) {
    public static PedidoResponse desde(Pedido p) {
        var u = p.getUsuario();
        return new PedidoResponse(
                p.getIdPedido(),
                p.getFechaPedido(),
                p.getEstado(),
                p.getTotal(),
                p.getDireccionEntrega(),
                u.getIdUsuario(),
                u.getNombre() + " " + u.getApellido(),
                p.getDetalles().stream().map(DetallePedidoResponse::desde).toList(),
                p.getPagos().stream().map(PagoResponse::desde).toList());
    }
}
