package e.commerce.digital.service;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.comprobante.ComprobanteResponse;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.ComprobanteMapper;
import e.commerce.digital.models.Comprobante;
import e.commerce.digital.models.Pago;
import e.commerce.digital.models.Pedido;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.ComprobanteRepository;
import e.commerce.digital.testutil.DatosDePrueba;
import e.commerce.digital.util.enumerated.EstadoPago;
import e.commerce.digital.util.enumerated.EstadoPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComprobanteService")
class ComprobanteServiceTest {

    private static final String EMAIL = "ana@mail.com";

    @Mock
    private ComprobanteRepository comprobanteRepository;

    private ComprobanteService service;
    private Pago pago;

    @BeforeEach
    void setUp() {
        service = new ComprobanteService(comprobanteRepository, new ComprobanteMapper());
        UsuarioCliente cliente = DatosDePrueba.cliente(1L, EMAIL);
        Pedido pedido = DatosDePrueba.pedido(10L, cliente, EstadoPedido.CONFIRMADO, "100.00");
        pago = DatosDePrueba.pago(7L, pedido, "100.00", EstadoPago.APROBADO);
    }

    private Comprobante comprobanteDe(Pago pago) {
        Comprobante c = new Comprobante();
        c.setIdComprobante(1L);
        c.setNumero("COMP-00000007");
        c.setTipo("COMPROBANTE_INTERNO");
        c.setFechaEmision(LocalDateTime.of(2026, 10, 13, 10, 0));
        c.setImporte(pago.getImporte());
        pago.setComprobante(c); // también enlaza c.pago
        return c;
    }

    @Test
    @DisplayName("emitir: genera un comprobante interno con número derivado del id del pago")
    void emitir_ok() {
        when(comprobanteRepository.save(any(Comprobante.class))).thenAnswer(inv -> inv.getArgument(0));

        Comprobante c = service.emitir(pago);

        assertThat(c.getNumero()).isEqualTo("COMP-00000007");
        assertThat(c.getTipo()).isEqualTo("COMPROBANTE_INTERNO");
        assertThat(c.getImporte()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(c.getFechaEmision()).isNotNull();
        assertThat(c.getPago()).isSameAs(pago);
        assertThat(pago.getComprobante()).isSameAs(c);
    }

    @Test
    @DisplayName("emitir: si el pago ya tiene comprobante, lo devuelve sin crear otro")
    void emitir_yaExiste() {
        Comprobante existente = comprobanteDe(pago);

        Comprobante c = service.emitir(pago);

        assertThat(c).isSameAs(existente);
        verify(comprobanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("listarMios: devuelve los comprobantes del cliente paginados")
    void listarMios() {
        Pageable pageable = PageRequest.of(0, 10);
        when(comprobanteRepository.findByPago_Pedido_Usuario_Email(EMAIL, pageable))
                .thenReturn(new PageImpl<>(List.of(comprobanteDe(pago)), pageable, 1));

        PageResponse<ComprobanteResponse> r = service.listarMios(EMAIL, pageable);

        assertThat(r.contenido()).hasSize(1);
        assertThat(r.contenido().get(0).numero()).isEqualTo("COMP-00000007");
        assertThat(r.contenido().get(0).idPago()).isEqualTo(7L);
    }

    @Test
    @DisplayName("obtenerMio: devuelve un comprobante propio")
    void obtenerMio_ok() {
        when(comprobanteRepository.findByIdComprobanteAndPago_Pedido_Usuario_Email(1L, EMAIL))
                .thenReturn(Optional.of(comprobanteDe(pago)));

        ComprobanteResponse r = service.obtenerMio(EMAIL, 1L);

        assertThat(r.idComprobante()).isEqualTo(1L);
        assertThat(r.importe()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("obtenerMio: un comprobante ajeno responde igual que uno inexistente")
    void obtenerMio_ajeno() {
        when(comprobanteRepository.findByIdComprobanteAndPago_Pedido_Usuario_Email(1L, EMAIL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerMio(EMAIL, 1L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("listarTodos: devuelve todos los comprobantes paginados (administrador)")
    void listarTodos() {
        Pageable pageable = PageRequest.of(0, 10);
        when(comprobanteRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(comprobanteDe(pago)), pageable, 1));

        assertThat(service.listarTodos(pageable).contenido()).hasSize(1);
    }

    @Test
    @DisplayName("obtenerAdmin: devuelve el comprobante o responde no encontrado")
    void obtenerAdmin() {
        when(comprobanteRepository.findById(1L)).thenReturn(Optional.of(comprobanteDe(pago)));
        when(comprobanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.obtenerAdmin(1L).numero()).isEqualTo("COMP-00000007");
        assertThatThrownBy(() -> service.obtenerAdmin(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
