package e.commerce.digital.service;

import e.commerce.digital.dto.carrito.CarritoItemCantidadRequest;
import e.commerce.digital.dto.carrito.CarritoItemRequest;
import e.commerce.digital.dto.carrito.CarritoResponse;
import e.commerce.digital.exception.ProductoNoDisponibleException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.exception.StockInsuficienteException;
import e.commerce.digital.mapper.CarritoMapper;
import e.commerce.digital.models.Carrito;
import e.commerce.digital.models.Producto;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.CarritoRepository;
import e.commerce.digital.repository.ProductoRepository;
import e.commerce.digital.repository.UsuarioClienteRepository;
import e.commerce.digital.testutil.DatosDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarritoService")
class CarritoServiceTest {

    private static final String EMAIL = "ana@mail.com";

    @Mock
    private CarritoRepository carritoRepository;
    @Mock
    private UsuarioClienteRepository usuarioClienteRepository;
    @Mock
    private ProductoRepository productoRepository;

    private CarritoService service;
    private UsuarioCliente cliente;
    private Carrito carrito;
    private Producto latex;

    @BeforeEach
    void setUp() {
        service = new CarritoService(carritoRepository, usuarioClienteRepository, productoRepository,
                new CarritoMapper());
        cliente = DatosDePrueba.cliente(1L, EMAIL);
        carrito = DatosDePrueba.carrito(10L, cliente);
        latex = DatosDePrueba.producto(5L, "Látex 20L", "15000.00", 10);
    }

    // ----- obtener -----

    @Test
    @DisplayName("obtener: devuelve el carrito existente con su total")
    void obtener_existente() {
        carrito.agregarItem(DatosDePrueba.item(100L, latex, 2));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));

        CarritoResponse r = service.obtener(EMAIL);

        assertThat(r.idCarrito()).isEqualTo(10L);
        assertThat(r.items()).hasSize(1);
        assertThat(r.total()).isEqualByComparingTo("30000.00");
    }

    @Test
    @DisplayName("obtener: si el cliente no tiene carrito, lo crea")
    void obtener_creaCarrito() {
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.empty());
        when(usuarioClienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));

        CarritoResponse r = service.obtener(EMAIL);

        assertThat(r.items()).isEmpty();
        assertThat(r.total()).isEqualByComparingTo("0");
        verify(carritoRepository).save(any(Carrito.class));
    }

    @Test
    @DisplayName("obtener: quien no es cliente (por ejemplo un administrador) no tiene carrito")
    void obtener_noEsCliente() {
        when(carritoRepository.findByUsuario_Email("luis@mail.com")).thenReturn(Optional.empty());
        when(usuarioClienteRepository.findByEmail("luis@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener("luis@mail.com"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(carritoRepository, never()).save(any());
    }

    // ----- agregarProducto -----

    @Test
    @DisplayName("agregarProducto: agrega un item nuevo con el precio vigente")
    void agregarProducto_nuevo() {
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(productoRepository.findById(5L)).thenReturn(Optional.of(latex));
        when(carritoRepository.saveAndFlush(carrito)).thenReturn(carrito);

        CarritoResponse r = service.agregarProducto(EMAIL, new CarritoItemRequest(5L, 2));

        assertThat(r.items()).hasSize(1);
        assertThat(r.items().get(0).idProducto()).isEqualTo(5L);
        assertThat(r.items().get(0).cantidad()).isEqualTo(2);
        assertThat(r.items().get(0).precioUnitario()).isEqualByComparingTo("15000.00");
        assertThat(r.total()).isEqualByComparingTo("30000.00");
        assertThat(carrito.getItems().get(0).getCarrito()).isSameAs(carrito);
    }

    @Test
    @DisplayName("agregarProducto: si el producto ya está, suma la cantidad y refresca el precio")
    void agregarProducto_sumaCantidad() {
        carrito.agregarItem(DatosDePrueba.item(100L, latex, 3));
        latex.setPrecio(new java.math.BigDecimal("16000.00")); // el catálogo subió el precio
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(productoRepository.findById(5L)).thenReturn(Optional.of(latex));
        when(carritoRepository.saveAndFlush(carrito)).thenReturn(carrito);

        CarritoResponse r = service.agregarProducto(EMAIL, new CarritoItemRequest(5L, 4));

        assertThat(r.items()).hasSize(1);
        assertThat(r.items().get(0).cantidad()).isEqualTo(7);
        assertThat(r.items().get(0).precioUnitario()).isEqualByComparingTo("16000.00");
    }

    @Test
    @DisplayName("agregarProducto: rechaza una cantidad mayor al stock")
    void agregarProducto_stockInsuficiente() {
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(productoRepository.findById(5L)).thenReturn(Optional.of(latex));

        assertThatThrownBy(() -> service.agregarProducto(EMAIL, new CarritoItemRequest(5L, 11)))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Látex 20L");

        verify(carritoRepository, never()).saveAndFlush(any());
        assertThat(carrito.getItems()).isEmpty();
    }

    @Test
    @DisplayName("agregarProducto: al sumar, la cantidad total tampoco puede superar el stock")
    void agregarProducto_sumaSuperaStock() {
        carrito.agregarItem(DatosDePrueba.item(100L, latex, 8));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(productoRepository.findById(5L)).thenReturn(Optional.of(latex));

        assertThatThrownBy(() -> service.agregarProducto(EMAIL, new CarritoItemRequest(5L, 3)))
                .isInstanceOf(StockInsuficienteException.class);

        assertThat(carrito.getItems().get(0).getCantidad()).isEqualTo(8);
    }

    @Test
    @DisplayName("agregarProducto: rechaza un producto desactivado")
    void agregarProducto_productoInactivo() {
        latex.setActivo(false);
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(productoRepository.findById(5L)).thenReturn(Optional.of(latex));

        assertThatThrownBy(() -> service.agregarProducto(EMAIL, new CarritoItemRequest(5L, 1)))
                .isInstanceOf(ProductoNoDisponibleException.class);

        verify(carritoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("agregarProducto: un producto inexistente responde no encontrado")
    void agregarProducto_productoNoExiste() {
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agregarProducto(EMAIL, new CarritoItemRequest(99L, 1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ----- actualizarCantidad -----

    @Test
    @DisplayName("actualizarCantidad: cambia la cantidad de un item propio")
    void actualizarCantidad_ok() {
        carrito.agregarItem(DatosDePrueba.item(100L, latex, 2));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(carritoRepository.saveAndFlush(carrito)).thenReturn(carrito);

        CarritoResponse r = service.actualizarCantidad(EMAIL, 100L, new CarritoItemCantidadRequest(6));

        assertThat(r.items().get(0).cantidad()).isEqualTo(6);
        assertThat(r.total()).isEqualByComparingTo("90000.00");
    }

    @Test
    @DisplayName("actualizarCantidad: rechaza una cantidad mayor al stock")
    void actualizarCantidad_stockInsuficiente() {
        carrito.agregarItem(DatosDePrueba.item(100L, latex, 2));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));

        assertThatThrownBy(() -> service.actualizarCantidad(EMAIL, 100L, new CarritoItemCantidadRequest(11)))
                .isInstanceOf(StockInsuficienteException.class);

        assertThat(carrito.getItems().get(0).getCantidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("actualizarCantidad: un item que no está en mi carrito responde no encontrado")
    void actualizarCantidad_itemAjeno() {
        carrito.agregarItem(DatosDePrueba.item(100L, latex, 2));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));

        assertThatThrownBy(() -> service.actualizarCantidad(EMAIL, 999L, new CarritoItemCantidadRequest(1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("actualizarCantidad: sin carrito responde no encontrado")
    void actualizarCantidad_sinCarrito() {
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarCantidad(EMAIL, 100L, new CarritoItemCantidadRequest(1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ----- quitar y vaciar -----

    @Test
    @DisplayName("quitarItem: elimina el item indicado y deja los demás")
    void quitarItem_ok() {
        Producto rodillo = DatosDePrueba.producto(6L, "Rodillo", "2500.00", 20);
        carrito.agregarItem(DatosDePrueba.item(100L, latex, 2));
        carrito.agregarItem(DatosDePrueba.item(101L, rodillo, 1));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(carritoRepository.saveAndFlush(carrito)).thenReturn(carrito);

        CarritoResponse r = service.quitarItem(EMAIL, 100L);

        assertThat(r.items()).extracting(i -> i.idProducto()).containsExactly(6L);
        assertThat(r.total()).isEqualByComparingTo("2500.00");
    }

    @Test
    @DisplayName("quitarItem: un item inexistente responde no encontrado")
    void quitarItem_noExiste() {
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));

        assertThatThrownBy(() -> service.quitarItem(EMAIL, 999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("vaciar: elimina todos los items")
    void vaciar_ok() {
        carrito.agregarItem(DatosDePrueba.item(100L, latex, 2));
        carrito.agregarItem(DatosDePrueba.item(101L, DatosDePrueba.producto(6L, "Rodillo", "2500.00", 20), 1));
        when(carritoRepository.findByUsuario_Email(EMAIL)).thenReturn(Optional.of(carrito));
        when(carritoRepository.saveAndFlush(carrito)).thenReturn(carrito);

        CarritoResponse r = service.vaciar(EMAIL);

        assertThat(r.items()).isEmpty();
        assertThat(r.total()).isEqualByComparingTo("0");
    }
}
