package e.commerce.digital.mappers;

import e.commerce.digital.dto.pedido.DetallePedidoResponse;
import e.commerce.digital.dto.pedido.PedidoRequest;
import e.commerce.digital.dto.pedido.PedidoResponse;
import e.commerce.digital.dto.pedido.PedidoResumenResponse;
import e.commerce.digital.models.CarritoItem;
import e.commerce.digital.models.DetallePedido;
import e.commerce.digital.models.Pedido;
import e.commerce.digital.models.UsuarioCliente;
import org.springframework.stereotype.Component;

@Component
public class PedidoMapper {

    // Pedido nuevo: estado PENDIENTE y total 0 por defecto; los detalles se agregan con pedido.agregarDetalle(...)
    public Pedido toEntity(UsuarioCliente cliente, PedidoRequest req) {
        Pedido pedido = new Pedido();
        pedido.setUsuario(cliente);
        pedido.setDireccionEntrega(req.direccionEntrega());
        return pedido;
    }

    // Convierte un item del carrito en detalle, copiando el precio vigente del producto
    // (el service ya debe haber revalidado precio y stock antes de llamar acá)
    public DetallePedido toDetalle(CarritoItem item) {
        DetallePedido d = new DetallePedido();
        d.setProducto(item.getProducto());
        d.setCantidad(item.getCantidad());
        d.setPrecioUnitario(item.getProducto().getPrecio());
        return d; // el subtotal lo calcula pedido.agregarDetalle(d)
    }

    public PedidoResponse toResponse(Pedido pedido) {
        return PedidoResponse.desde(pedido);
    }

    public PedidoResumenResponse toResumen(Pedido pedido) {
        return PedidoResumenResponse.desde(pedido);
    }

    public DetallePedidoResponse toDetalleResponse(DetallePedido detalle) {
        return DetallePedidoResponse.desde(detalle);
    }
}