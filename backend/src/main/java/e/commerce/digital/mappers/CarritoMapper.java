package e.commerce.digital.mappers;

import e.commerce.digital.dto.carrito.CarritoItemResponse;
import e.commerce.digital.dto.carrito.CarritoResponse;
import e.commerce.digital.models.Carrito;
import e.commerce.digital.models.CarritoItem;
import e.commerce.digital.models.Producto;
import org.springframework.stereotype.Component;

@Component
public class CarritoMapper {

    // Crea el item con el precio vigente del producto como precio de referencia.
    // Para asociarlo al carrito usar carrito.agregarItem(item) en el service.
    public CarritoItem toItem(Producto producto, Integer cantidad) {
        CarritoItem item = new CarritoItem();
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(producto.getPrecio());
        return item;
    }

    public CarritoResponse toResponse(Carrito carrito) {
        return CarritoResponse.desde(carrito);
    }

    public CarritoItemResponse toItemResponse(CarritoItem item) {
        return CarritoItemResponse.desde(item);
    }
}