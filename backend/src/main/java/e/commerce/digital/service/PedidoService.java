package e.commerce.digital.service;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.pedido.CambioEstadoPedidoRequest;
import e.commerce.digital.dto.pedido.PedidoFiltroRequest;
import e.commerce.digital.dto.pedido.PedidoRequest;
import e.commerce.digital.dto.pedido.PedidoResponse;
import e.commerce.digital.dto.pedido.PedidoResumenResponse;
import e.commerce.digital.exception.OperacionInvalidaException;
import e.commerce.digital.exception.ProductoNoDisponibleException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.exception.StockInsuficienteException;
import e.commerce.digital.exception.TransicionInvalidaException;
import e.commerce.digital.mapper.PedidoMapper;
import e.commerce.digital.models.Carrito;
import e.commerce.digital.models.CarritoItem;
import e.commerce.digital.models.DetallePedido;
import e.commerce.digital.models.Pedido;
import e.commerce.digital.models.Producto;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.CarritoRepository;
import e.commerce.digital.repository.PagoRepository;
import e.commerce.digital.repository.PedidoRepository;
import e.commerce.digital.repository.ProductoRepository;
import e.commerce.digital.repository.UsuarioClienteRepository;
import e.commerce.digital.util.enumerated.EstadoPago;
import e.commerce.digital.util.enumerated.EstadoPedido;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static e.commerce.digital.util.enumerated.EstadoPedido.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoService {

    // Transiciones permitidas; ENTREGADO y CANCELADO son estados finales (no
    // figuran como origen)
    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            PENDIENTE, Set.of(CONFIRMADO, CANCELADO),
            CONFIRMADO, Set.of(EN_PREPARACION, CANCELADO),
            EN_PREPARACION, Set.of(ENVIADO, CANCELADO),
            ENVIADO, Set.of(ENTREGADO));

    private final PedidoRepository pedidoRepository;
    private final CarritoRepository carritoRepository;
    private final UsuarioClienteRepository usuarioClienteRepository;
    private final ProductoRepository productoRepository;
    private final PagoRepository pagoRepository;
    private final PedidoMapper pedidoMapper;

    // ----- Cliente -----

    /**
     * Convierte el carrito del cliente autenticado en un pedido, en una sola
     * transacción:
     * revalida disponibilidad, descuenta stock de forma atómica, copia los precios
     * vigentes,
     * calcula el total en el servidor y vacía el carrito. Si algo falla, se
     * revierten todas las operaciones.
     */
    @Transactional
    public PedidoResponse confirmar(String email, PedidoRequest req) {
        UsuarioCliente cliente = usuarioClienteRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new OperacionInvalidaException("La cuenta está desactivada");
        }

        Carrito carrito = carritoRepository.findByUsuario_Email(email)
                .orElseThrow(() -> new OperacionInvalidaException("El carrito está vacío"));
        if (carrito.getItems().isEmpty()) {
            throw new OperacionInvalidaException("El carrito está vacío");
        }

        Pedido pedido = pedidoMapper.toEntity(cliente, req);

        // Se procesa en orden de producto para que pedidos simultáneos no se bloqueen
        // entre sí (deadlock)
        List<CarritoItem> items = carrito.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProducto().getIdProducto()))
                .toList();

        for (CarritoItem item : items) {
            Producto producto = item.getProducto();
            if (!producto.estaDisponible()) {
                throw new ProductoNoDisponibleException(producto.getNombre());
            }
            // UPDATE atómico: descuenta solo si hay stock suficiente (devuelve 0 filas si
            // no)
            if (productoRepository.descontarStock(producto.getIdProducto(), item.getCantidad()) == 0) {
                throw new StockInsuficienteException(producto.getNombre());
            }
            // El detalle copia el precio vigente del producto, no el de referencia del
            // carrito
            pedido.agregarDetalle(pedidoMapper.toDetalle(item));
        }

        Pedido guardado = pedidoRepository.save(pedido);
        carrito.getItems().clear(); // orphanRemoval vacía el carrito
        return pedidoMapper.toResponse(guardado);
    }

    public PageResponse<PedidoResumenResponse> listarMios(String email, Pageable pageable) {
        return PageResponse.desde(pedidoRepository.findByUsuario_Email(email, pageable)
                .map(pedidoMapper::toResumen));
    }

    /**
     * Si el pedido no existe o es de otro usuario, responde igual: no encontrado.
     */
    public PedidoResponse obtenerMio(String email, Long idPedido) {
        Pedido pedido = pedidoRepository.findByIdPedidoAndUsuario_Email(idPedido, email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
        return pedidoMapper.toResponse(pedido);
    }

    // ----- Administración -----

    public PageResponse<PedidoResumenResponse> filtrar(PedidoFiltroRequest filtro, Pageable pageable) {
        LocalDateTime desde = null;
        LocalDateTime hasta = null;
        EstadoPedido estado = null;
        Long idUsuario = null;
        if (filtro != null) {
            desde = filtro.desde() == null ? null : filtro.desde().atStartOfDay();
            // "hasta" incluye el día completo: la consulta usa límite superior exclusivo
            hasta = filtro.hasta() == null ? null : filtro.hasta().plusDays(1).atStartOfDay();
            estado = filtro.estado();
            idUsuario = filtro.idUsuario();
        }
        return PageResponse.desde(pedidoRepository.filtrar(estado, idUsuario, desde, hasta, pageable)
                .map(pedidoMapper::toResumen));
    }

    public PedidoResponse obtenerAdmin(Long idPedido) {
        return pedidoMapper.toResponse(buscar(idPedido));
    }

    /**
     * Cambia el estado validando la transición.
     * - CONFIRMADO exige pagos aprobados que cubran el total.
     * - CANCELADO repone el stock (solo es posible antes del envío).
     */
    @Transactional
    public PedidoResponse cambiarEstado(Long idPedido, CambioEstadoPedidoRequest req) {
        Pedido pedido = buscar(idPedido);
        EstadoPedido actual = pedido.getEstado();
        EstadoPedido nuevo = req.estado();

        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevo)) {
            throw new TransicionInvalidaException(actual, nuevo);
        }

        if (nuevo == CONFIRMADO) {
            BigDecimal aprobado = pagoRepository.sumarImportePorPedidoYEstado(idPedido, EstadoPago.APROBADO);
            if (aprobado.compareTo(pedido.getTotal()) < 0) {
                throw new OperacionInvalidaException("El pedido no tiene pagos aprobados que cubran el total");
            }
        }

        if (nuevo == CANCELADO) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                productoRepository.reponerStock(detalle.getProducto().getIdProducto(), detalle.getCantidad());
            }
        }

        pedido.setEstado(nuevo);
        return pedidoMapper.toResponse(pedido);
    }

    private Pedido buscar(Long idPedido) {
        return pedidoRepository.findWithDetallesByIdPedido(idPedido)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
    }
}