package e.commerce.digital.service;

import e.commerce.digital.dto.carrito.CarritoItemCantidadRequest;
import e.commerce.digital.dto.carrito.CarritoItemRequest;
import e.commerce.digital.dto.carrito.CarritoResponse;
import e.commerce.digital.exception.ProductoNoDisponibleException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.exception.StockInsuficienteException;
import e.commerce.digital.mapper.CarritoMapper;
import e.commerce.digital.models.Carrito;
import e.commerce.digital.models.CarritoItem;
import e.commerce.digital.models.Producto;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.CarritoRepository;
import e.commerce.digital.repository.ProductoRepository;
import e.commerce.digital.repository.UsuarioClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Todas las operaciones reciben el email del usuario autenticado (nunca un id
 * del cliente),
 * por lo que un cliente solo puede tocar su propio carrito.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final UsuarioClienteRepository usuarioClienteRepository;
    private final ProductoRepository productoRepository;
    private final CarritoMapper carritoMapper;

    public CarritoResponse obtener(String email) {
        return carritoMapper.toResponse(obtenerOCrear(email));
    }

    /**
     * Agrega un producto; si ya está en el carrito, suma la cantidad. Verifica
     * disponibilidad y stock.
     */
    public CarritoResponse agregarProducto(String email, CarritoItemRequest req) {
        Carrito carrito = obtenerOCrear(email);
        Producto producto = productoRepository.findById(req.idProducto())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
        validarDisponible(producto);

        CarritoItem existente = buscarItemPorProducto(carrito, producto.getIdProducto());
        int cantidadTotal = req.cantidad() + (existente == null ? 0 : existente.getCantidad());
        validarStock(producto, cantidadTotal);

        if (existente != null) {
            existente.setCantidad(cantidadTotal);
            existente.setPrecioUnitario(producto.getPrecio()); // se refresca el precio de referencia
        } else {
            carrito.agregarItem(carritoMapper.toItem(producto, req.cantidad()));
        }
        // saveAndFlush: asegura que el item nuevo ya tenga id al armar la respuesta
        return carritoMapper.toResponse(carritoRepository.saveAndFlush(carrito));
    }

    public CarritoResponse actualizarCantidad(String email, Long idItem, CarritoItemCantidadRequest req) {
        Carrito carrito = buscarCarrito(email);
        CarritoItem item = buscarItem(carrito, idItem);
        Producto producto = item.getProducto();
        validarDisponible(producto);
        validarStock(producto, req.cantidad());

        item.setCantidad(req.cantidad());
        item.setPrecioUnitario(producto.getPrecio());
        return carritoMapper.toResponse(carritoRepository.saveAndFlush(carrito));
    }

    public CarritoResponse quitarItem(String email, Long idItem) {
        Carrito carrito = buscarCarrito(email);
        carrito.quitarItem(buscarItem(carrito, idItem)); // orphanRemoval borra el item
        return carritoMapper.toResponse(carritoRepository.saveAndFlush(carrito));
    }

    public CarritoResponse vaciar(String email) {
        Carrito carrito = buscarCarrito(email);
        carrito.getItems().clear();
        return carritoMapper.toResponse(carritoRepository.saveAndFlush(carrito));
    }

    // ----- Auxiliares -----

    private Carrito buscarCarrito(String email) {
        return carritoRepository.findByUsuario_Email(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Carrito no encontrado"));
    }

    // Normalmente el carrito se crea al registrarse; esto cubre clientes que aún no
    // lo tengan
    private Carrito obtenerOCrear(String email) {
        return carritoRepository.findByUsuario_Email(email).orElseGet(() -> {
            UsuarioCliente cliente = usuarioClienteRepository.findByEmail(email)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Solo los clientes tienen carrito"));
            Carrito carrito = new Carrito();
            carrito.setUsuario(cliente);
            cliente.setCarrito(carrito);
            return carritoRepository.save(carrito);
        });
    }

    private CarritoItem buscarItem(Carrito carrito, Long idItem) {
        return carrito.getItems().stream()
                .filter(i -> i.getIdCarritoItem().equals(idItem))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El item no existe en su carrito"));
    }

    private CarritoItem buscarItemPorProducto(Carrito carrito, Long idProducto) {
        return carrito.getItems().stream()
                .filter(i -> i.getProducto().getIdProducto().equals(idProducto))
                .findFirst()
                .orElse(null);
    }

    private void validarDisponible(Producto producto) {
        if (!producto.estaDisponible()) {
            throw new ProductoNoDisponibleException(producto.getNombre());
        }
    }

    private void validarStock(Producto producto, int cantidad) {
        if (producto.getStock() < cantidad) {
            throw new StockInsuficienteException(producto.getNombre());
        }
    }
}