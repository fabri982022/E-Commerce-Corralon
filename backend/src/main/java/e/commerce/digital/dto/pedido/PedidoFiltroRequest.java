package e.commerce.digital.dto.pedido;

import e.commerce.digital.util.enumerated.EstadoPedido;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// Filtros del administrador sobre los pedidos del negocio (por fecha, estado y cliente); todos opcionales
public record PedidoFiltroRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        EstadoPedido estado,
        Long idUsuario
) {
}
