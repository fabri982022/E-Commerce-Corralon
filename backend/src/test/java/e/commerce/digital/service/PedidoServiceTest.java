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
import e.commerce.digital.models.DetallePedido;
import e.commerce.digital.models.Pedido;
import e.commerce.digital.models.Producto;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.CarritoRepository;
import e.commerce.digital.repository.PagoRepository;
import e.commerce.digital.repository.PedidoRepository;
import e.commerce.digital.repository.ProductoRepository;
import e.commerce.digital.repository.UsuarioClienteRepository;
import e.commerce.digital.testutil.DatosDePrueba;
import e.commerce.digital.util.enumerated.EstadoPago;
import e.commerce.digital.util.enumerated.EstadoPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService")
class PedidoServiceTest {

    private static final String EMAIL = "ana@mail.com";

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private CarritoRepository carritoRepository;
    @Mock
    private UsuarioClienteRepository usuarioClienteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PagoRepository pagoRepository;

    private PedidoService service;
    private UsuarioCliente cliente;
    private Carrito carrito;
    private Producto latex;     // id 1, $1000.00
    private Producto rodillo;   // id 2, $250.50

    @BeforeEach
    void setUp() {
        service = new PedidoService(pedidoRepository, carritoRepository, usuarioClienteRepository,
                productoRepository, pagoRepository, new PedidoMapper());
        cliente = DatosDePrueba.cliente(1L, EMAIL);
        carrito = DatosDePrueba.carrito(10L, cliente);
        latex = DatosDePrueba.producto(1L, "Látex", "1000.00", 10);
        rodillo = DatosDePrueba.producto(2L, "Rodillo", "250.50", 5);
    }

    // =====================  confirmar  =====================

    @Test
    @DisplayName("confirmar: crea el pedido, descuenta stock en orden de producto, calcula el total y vacía el carrito")
    void confirmar_ok() {
        // el rodillo (id 2) se agrega primero para comprobar que el descuento se hace por id ascendente
        carrito.agregarItem(DatosDePrueba.item(100L, rodillo, 2));
        carrito.agregarItem(DatosDePrueba.item(101L, latex, 3));
        when(usuarioClienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(productoRepository.descontarStock(1L, 3)).thenReturn(1);
        when(productoRepository.descontarStock(2L, 2)).thenReturn(1);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoResponse r = service.confirmar(EMAIL, new PedidoRequest("Calle 123"));

        InOrder orden = inOrder(productoRepository);
        orden.verify(productoRepository).descontarStock(1L, 3);
        orden.verify(productoRepository).descontarStock(2L, 2);

        assertThat(r.estado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(r.direccionEntrega()).isEqualTo("Calle 123");
        assertThat(r.idUsuario()).isEqualTo(1L);
        assertThat(r.detalles()).hasSize(2);
        assertThat(r.total()).isEqualByComparingTo("3501.00"); // 3 x 1000.00 + 2 x 250.50
        assertThat(r.pagos()).isEmpty();
        assertThat(carrito.getItems()).isEmpty();
    }

    @Test
    @DisplayName("confirmar: cada detalle copia el precio vigente del producto, no el del carrito")
    void confirmar_usaPrecioVigente() {
        carrito.agregarItem(DatosDePrueba.item(101L, latex, 1)); // el item guardó 1000.00
        latex.setPrecio(new BigDecimal("1200.00"));              // el catálogo subió el precio
        when(usuarioClienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(productoRepository.descontarStock(1L, 1)).thenReturn(1);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoResponse r = service.confirmar(EMAIL, new PedidoRequest("Calle 123"));

        assertThat(r.detalles().get(0).precioUnitario()).isEqualByComparingTo("1200.00");
        assertThat(r.detalles().get(0).subtotal()).isEqualByComparingTo("1200.00");
        assertThat(r.total()).isEqualByComparingTo("1200.00");
    }

    @Test
    @DisplayName("confirmar: un cliente inexistente responde no encontrado")
    void confirmar_clienteNoExiste() {
        when(usuarioClienteRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmar(EMAIL, new PedidoRequest("Calle 123")))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(carritoRepository, productoRepository, pedidoRepository);
    }

    @Test
    @DisplayName("confirmar: una cuenta desactivada no puede comprar")
    void confirmar_cuentaDesactivada() {
        cliente.setActivo(false);
        when(usuarioClienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> service.confirmar(EMAIL, new PedidoRequest("Calle 123")))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("desactivada");

        verifyNoInteractions(carritoRepository, productoRepository, pedidoRepository);
    }

    @Test
    @DisplayName("confirmar: sin carrito se rechaza")
    void confirmar_sinCarrito() {
        when(usuarioClienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmar(EMAIL, new PedidoRequest("Calle 123")))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    @DisplayName("confirmar: con el carrito vacío se rechaza")
    void confirmar_carritoVacio() {
        when(usuarioClienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));

        assertThatThrownBy(() -> service.confirmar(EMAIL, new PedidoRequest("Calle 123")))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("vacío");

        verifyNoInteractions(productoRepository);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmar: un producto desactivado impide la compra y no descuenta stock")
    void confirmar_productoDesactivado() {
        latex.setActivo(false);
        carrito.agregarItem(DatosDePrueba.item(101L, latex, 1));
        when(usuarioClienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));

        assertThatThrownBy(() -> service.confirmar(EMAIL, new PedidoRequest("Calle 123")))
                .isInstanceOf(ProductoNoDisponibleException.class)
                .hasMessageContaining("Látex");

        verify(productoRepository, never()).descontarStock(anyLong(), anyInt());
        verify(pedidoRepository, never()).save(any());
        assertThat(carrito.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("confirmar: sin stock suficiente no se guarda el pedido")
    void confirmar_sinStock() {
        carrito.agregarItem(DatosDePrueba.item(101L, latex, 3));
        when(usuarioClienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(productoRepository.descontarStock(1L, 3)).thenReturn(0);

        assertThatThrownBy(() -> service.confirmar(EMAIL, new PedidoRequest("Calle 123")))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Látex");

        verify(pedidoRepository, never()).save(any());
        assertThat(carrito.getItems()).hasSize(1);
    }

    // =====================  consultas del cliente  =====================

    @Test
    @DisplayName("listarMios: devuelve el historial paginado del cliente")
    void listarMios() {
        Pageable pageable = PageRequest.of(0, 10);
        Pedido pedido = DatosDePrueba.pedido(7L, cliente, EstadoPedido.ENTREGADO, "500.00");
        when(pedidoRepository.findByUsuario_Email(EMAIL, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        PageResponse<PedidoResumenResponse> r = service.listarMios(EMAIL, pageable);

        assertThat(r.contenido()).hasSize(1);
        assertThat(r.contenido().get(0).idPedido()).isEqualTo(7L);
        assertThat(r.contenido().get(0).estado()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(r.totalElementos()).isEqualTo(1);
    }

    @Test
    @DisplayName("obtenerMio: devuelve un pedido propio")
    void obtenerMio_ok() {
        Pedido pedido = DatosDePrueba.pedido(7L, cliente, EstadoPedido.PENDIENTE, "500.00");
        when(pedidoRepository.findByIdPedidoAndUsuario_Email(7L, EMAIL)).thenReturn(Optional.of(pedido));

        assertThat(service.obtenerMio(EMAIL, 7L).idPedido()).isEqualTo(7L);
    }

    @Test
    @DisplayName("obtenerMio: un pedido ajeno responde igual que uno inexistente")
    void obtenerMio_ajeno() {
        when(pedidoRepository.findByIdPedidoAndUsuario_Email(7L, EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerMio(EMAIL, 7L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Pedido");
    }

    // =====================  administración  =====================

    @Test
    @DisplayName("filtrar: convierte las fechas, con 'hasta' incluyendo el día completo")
    void filtrar_convierteFechas() {
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.filtrar(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        DatosDePrueba.pedido(7L, cliente, EstadoPedido.ENVIADO, "500.00")), pageable, 1));

        PageResponse<PedidoResumenResponse> r = service.filtrar(
                new PedidoFiltroRequest(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                        EstadoPedido.ENVIADO, 1L), pageable);

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(pedidoRepository).filtrar(eq(EstadoPedido.ENVIADO), eq(1L), desde.capture(), hasta.capture(),
                eq(pageable));

        assertThat(desde.getValue()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(hasta.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0)); // límite exclusivo
        assertThat(r.contenido()).hasSize(1);
    }

    @Test
    @DisplayName("filtrar: un filtro nulo equivale a no filtrar")
    void filtrar_filtroNulo() {
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.filtrar(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.filtrar(null, pageable);

        verify(pedidoRepository).filtrar(isNull(), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    @DisplayName("obtenerAdmin: un pedido inexistente responde no encontrado")
    void obtenerAdmin_noExiste() {
        when(pedidoRepository.findWithDetallesByIdPedido(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerAdmin(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =====================  cambiarEstado  =====================

    private Pedido pedidoEn(EstadoPedido estado) {
        Pedido pedido = DatosDePrueba.pedido(1L, cliente, estado, "1000.00");
        when(pedidoRepository.findWithDetallesByIdPedido(1L)).thenReturn(Optional.of(pedido));
        return pedido;
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("cambiarEstado: transiciones permitidas sin condiciones adicionales")
    @CsvSource({
            "CONFIRMADO,EN_PREPARACION",
            "EN_PREPARACION,ENVIADO",
            "ENVIADO,ENTREGADO"
    })
    void cambiarEstado_transicionesPermitidas(EstadoPedido actual, EstadoPedido nuevo) {
        Pedido pedido = pedidoEn(actual);

        PedidoResponse r = service.cambiarEstado(1L, new CambioEstadoPedidoRequest(nuevo));

        assertThat(r.estado()).isEqualTo(nuevo);
        assertThat(pedido.getEstado()).isEqualTo(nuevo);
        verifyNoInteractions(productoRepository, pagoRepository);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("cambiarEstado: rechaza las transiciones no permitidas")
    @CsvSource({
            "PENDIENTE,ENVIADO",
            "PENDIENTE,ENTREGADO",
            "PENDIENTE,PENDIENTE",
            "CONFIRMADO,PENDIENTE",
            "CONFIRMADO,ENVIADO",
            "EN_PREPARACION,CONFIRMADO",
            "ENVIADO,CANCELADO",
            "ENVIADO,PENDIENTE",
            "ENTREGADO,CANCELADO",
            "CANCELADO,PENDIENTE",
            "CANCELADO,CONFIRMADO"
    })
    void cambiarEstado_transicionesInvalidas(EstadoPedido actual, EstadoPedido nuevo) {
        Pedido pedido = pedidoEn(actual);

        assertThatThrownBy(() -> service.cambiarEstado(1L, new CambioEstadoPedidoRequest(nuevo)))
                .isInstanceOf(TransicionInvalidaException.class);

        assertThat(pedido.getEstado()).isEqualTo(actual);
        verifyNoInteractions(productoRepository, pagoRepository);
    }

    @Test
    @DisplayName("cambiarEstado: confirmar exige pagos aprobados que cubran el total")
    void cambiarEstado_confirmarConPagoAprobado() {
        Pedido pedido = pedidoEn(EstadoPedido.PENDIENTE);
        when(pagoRepository.sumarImportePorPedidoYEstado(1L, EstadoPago.APROBADO))
                .thenReturn(new BigDecimal("1000.00"));

        PedidoResponse r = service.cambiarEstado(1L, new CambioEstadoPedidoRequest(EstadoPedido.CONFIRMADO));

        assertThat(r.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CONFIRMADO);
    }

    @Test
    @DisplayName("cambiarEstado: no se puede confirmar si los pagos aprobados no alcanzan")
    void cambiarEstado_confirmarSinPagoSuficiente() {
        Pedido pedido = pedidoEn(EstadoPedido.PENDIENTE);
        when(pagoRepository.sumarImportePorPedidoYEstado(1L, EstadoPago.APROBADO))
                .thenReturn(new BigDecimal("400.00"));

        assertThatThrownBy(() -> service.cambiarEstado(1L, new CambioEstadoPedidoRequest(EstadoPedido.CONFIRMADO)))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("pagos aprobados");

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
    }

    @Test
    @DisplayName("cambiarEstado: cancelar repone el stock de cada detalle")
    void cambiarEstado_cancelarReponeStock() {
        Pedido pedido = pedidoEn(EstadoPedido.CONFIRMADO);
        pedido.agregarDetalle(detalle(latex, 3));
        pedido.agregarDetalle(detalle(rodillo, 2));

        PedidoResponse r = service.cambiarEstado(1L, new CambioEstadoPedidoRequest(EstadoPedido.CANCELADO));

        assertThat(r.estado()).isEqualTo(EstadoPedido.CANCELADO);
        verify(productoRepository).reponerStock(1L, 3);
        verify(productoRepository).reponerStock(2L, 2);
    }

    @Test
    @DisplayName("cambiarEstado: un pedido inexistente responde no encontrado")
    void cambiarEstado_noExiste() {
        when(pedidoRepository.findWithDetallesByIdPedido(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(99L, new CambioEstadoPedidoRequest(EstadoPedido.CANCELADO)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private DetallePedido detalle(Producto producto, int cantidad) {
        DetallePedido d = new DetallePedido();
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(producto.getPrecio());
        return d;
    }
}
