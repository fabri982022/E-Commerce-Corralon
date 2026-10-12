package e.commerce.digital.dto.pedido;

import e.commerce.digital.models.Pedido;
import e.commerce.digital.util.enumerated.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Para listados y paginación del historial de compras
public record PedidoResumenResponse(
        Long idPedido,
        LocalDateTime fechaPedido,
        EstadoPedido estado,
        BigDecimal total,
        Long idUsuario,
        String nombreCliente
) {
    public static PedidoResumenResponse desde(Pedido p) {
        var u = p.getUsuario();
        return new PedidoResumenResponse(
                p.getIdPedido(),
                p.getFechaPedido(),
                p.getEstado(),
                p.getTotal(),
                u.getIdUsuario(),
                u.getNombre() + " " + u.getApellido());
    }
}
