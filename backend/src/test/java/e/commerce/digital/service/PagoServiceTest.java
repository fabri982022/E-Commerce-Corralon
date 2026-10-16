package e.commerce.digital.service;

import e.commerce.digital.dto.pago.CambioEstadoPagoRequest;
import e.commerce.digital.dto.pago.PagoRequest;
import e.commerce.digital.dto.pago.PagoResponse;
import e.commerce.digital.exception.OperacionInvalidaException;
import e.commerce.digital.exception.PagoYaProcesadoException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.PagoMapper;
import e.commerce.digital.models.Pago;
import e.commerce.digital.models.Pedido;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.PagoRepository;
import e.commerce.digital.repository.PedidoRepository;
import e.commerce.digital.testutil.DatosDePrueba;
import e.commerce.digital.util.enumerated.EstadoPago;
import e.commerce.digital.util.enumerated.EstadoPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoService")
class PagoServiceTest {

    private static final String EMAIL = "ana@mail.com";

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ComprobanteService comprobanteService;

    private PagoService service;
    private UsuarioCliente cliente;
    private Pedido pedido; // pedido 10, PENDIENTE, total 100.00

    @BeforeEach
    void setUp() {
        service = new PagoService(pagoRepository, pedidoRepository, new PagoMapper(), comprobanteService);
        cliente = DatosDePrueba.cliente(1L, EMAIL);
        pedido = DatosDePrueba.pedido(10L, cliente, EstadoPedido.PENDIENTE, "100.00");
    }

    private void aprobadoPrevio(String importe) {
        when(pagoRepository.sumarImportePorPedidoYEstado(10L, EstadoPago.APROBADO))
                .thenReturn(new BigDecimal(importe));
    }

    private PagoRequest solicitud(String importe) {
        return new PagoRequest("TRANSFERENCIA", new BigDecimal(importe), "REF-1");
    }

    // =====================  registrar  =====================

    @Test
    @DisplayName("registrarComoCliente: el pago nace PENDIENTE y queda asociado al pedido")
    void registrarComoCliente_ok() {
        when(pedidoRepository.findByIdPedidoAndUsuario_Email(10L, EMAIL)).thenReturn(Optional.of(pedido));
        aprobadoPrevio("0");
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoResponse r = service.registrarComoCliente(EMAIL, 10L, solicitud("60.00"));

        assertThat(r.estado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(r.importe()).isEqualByComparingTo("60.00");
        assertThat(r.metodo()).isEqualTo("TRANSFERENCIA");
        assertThat(r.referenciaOperacion()).isEqualTo("REF-1");
        assertThat(r.idPedido()).isEqualTo(10L);
        assertThat(r.fechaPago()).isNull();
        assertThat(r.comprobante()).isNull();
        assertThat(pedido.getPagos()).hasSize(1);
    }

    @Test
    @DisplayName("registrarComoCliente: un pedido ajeno responde no encontrado")
    void registrarComoCliente_pedidoAjeno() {
        when(pedidoRepository.findByIdPedidoAndUsuario_Email(10L, EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarComoCliente(EMAIL, 10L, solicitud("60.00")))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(pagoRepository);
    }

    @Test
    @DisplayName("registrarComoAdmin: registra un pago manual sobre cualquier pedido")
    void registrarComoAdmin_ok() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        aprobadoPrevio("0");
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoResponse r = service.registrarComoAdmin(10L, solicitud("100.00"));

        assertThat(r.estado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(r.importe()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("registrarComoAdmin: un pedido inexistente responde no encontrado")
    void registrarComoAdmin_pedidoNoExiste() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarComoAdmin(10L, solicitud("10.00")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("registrar: no se admiten pagos sobre un pedido cancelado")
    void registrar_pedidoCancelado() {
        pedido.setEstado(EstadoPedido.CANCELADO);
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.registrarComoAdmin(10L, solicitud("10.00")))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("cancelado");

        verifyNoInteractions(pagoRepository);
    }

    @Test
    @DisplayName("registrar: un pedido ya pagado responde pago ya procesado")
    void registrar_pedidoYaPagado() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        aprobadoPrevio("100.00");

        assertThatThrownBy(() -> service.registrarComoAdmin(10L, solicitud("10.00")))
                .isInstanceOf(PagoYaProcesadoException.class);

        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar: el importe no puede superar el saldo pendiente")
    void registrar_importeSuperaSaldo() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        aprobadoPrevio("30.00"); // saldo = 70.00

        assertThatThrownBy(() -> service.registrarComoAdmin(10L, solicitud("80.00")))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("saldo")
                .hasMessageContaining("70.00");

        verify(pagoRepository, never()).save(any());
    }

    // =====================  cambiarEstado  =====================

    private Pago pagoPendiente(String importe) {
        Pago pago = DatosDePrueba.pago(7L, pedido, importe, EstadoPago.PENDIENTE);
        when(pagoRepository.findById(7L)).thenReturn(Optional.of(pago));
        return pago;
    }

    @Test
    @DisplayName("aprobar: si cubre el total, registra la fecha, emite el comprobante y confirma el pedido")
    void aprobar_cubreElTotal() {
        Pago pago = pagoPendiente("100.00");
        aprobadoPrevio("0");

        PagoResponse r = service.cambiarEstado(7L, new CambioEstadoPagoRequest(EstadoPago.APROBADO));

        assertThat(r.estado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(pago.getFechaPago()).isNotNull();
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CONFIRMADO);
        verify(comprobanteService).emitir(pago);
    }

    @Test
    @DisplayName("aprobar: un pago parcial emite comprobante pero el pedido sigue PENDIENTE")
    void aprobar_pagoParcial() {
        Pago pago = pagoPendiente("40.00");
        aprobadoPrevio("0");

        service.cambiarEstado(7L, new CambioEstadoPagoRequest(EstadoPago.APROBADO));

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        verify(comprobanteService).emitir(pago);
    }

    @Test
    @DisplayName("aprobar: el pago que completa el total confirma el pedido")
    void aprobar_completaElTotal() {
        pagoPendiente("40.00");
        aprobadoPrevio("60.00");

        service.cambiarEstado(7L, new CambioEstadoPagoRequest(EstadoPago.APROBADO));

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CONFIRMADO);
    }

    @Test
    @DisplayName("aprobar: los pagos aprobados no pueden superar el total del pedido")
    void aprobar_superaElTotal() {
        Pago pago = pagoPendiente("40.00");
        aprobadoPrevio("80.00");

        assertThatThrownBy(() -> service.cambiarEstado(7L, new CambioEstadoPagoRequest(EstadoPago.APROBADO)))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("superarían");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(pago.getFechaPago()).isNull();
        verify(comprobanteService, never()).emitir(any(Pago.class));
    }

    @Test
    @DisplayName("aprobar: no se aprueba el pago de un pedido cancelado")
    void aprobar_pedidoCancelado() {
        pedido.setEstado(EstadoPedido.CANCELADO);
        Pago pago = pagoPendiente("100.00");

        assertThatThrownBy(() -> service.cambiarEstado(7L, new CambioEstadoPagoRequest(EstadoPago.APROBADO)))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("cancelado");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        verify(comprobanteService, never()).emitir(any(Pago.class));
    }

    @Test
    @DisplayName("rechazar: el pago pasa a RECHAZADO sin comprobante ni cambios en el pedido")
    void rechazar_ok() {
        Pago pago = pagoPendiente("100.00");

        PagoResponse r = service.cambiarEstado(7L, new CambioEstadoPagoRequest(EstadoPago.RECHAZADO));

        assertThat(r.estado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(pago.getFechaPago()).isNull();
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        verifyNoInteractions(comprobanteService);
    }

    @Test
    @DisplayName("reembolsar: un pago aprobado pasa a REEMBOLSADO y no toca el estado del pedido")
    void reembolsar_ok() {
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        Pago pago = DatosDePrueba.pago(7L, pedido, "100.00", EstadoPago.APROBADO);
        when(pagoRepository.findById(7L)).thenReturn(Optional.of(pago));

        PagoResponse r = service.cambiarEstado(7L, new CambioEstadoPagoRequest(EstadoPago.REEMBOLSADO));

        assertThat(r.estado()).isEqualTo(EstadoPago.REEMBOLSADO);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CONFIRMADO);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("cambiarEstado: un pago ya procesado no admite más cambios")
    @CsvSource({
            "RECHAZADO,APROBADO",
            "RECHAZADO,RECHAZADO",
            "REEMBOLSADO,APROBADO",
            "REEMBOLSADO,REEMBOLSADO",
            "APROBADO,APROBADO",
            "APROBADO,RECHAZADO",
            "APROBADO,PENDIENTE"
    })
    void cambiarEstado_pagoYaProcesado(EstadoPago actual, EstadoPago nuevo) {
        Pago pago = DatosDePrueba.pago(7L, pedido, "100.00", actual);
        when(pagoRepository.findById(7L)).thenReturn(Optional.of(pago));

        assertThatThrownBy(() -> service.cambiarEstado(7L, new CambioEstadoPagoRequest(nuevo)))
                .isInstanceOf(PagoYaProcesadoException.class)
                .hasMessageContaining(actual.name());

        assertThat(pago.getEstado()).isEqualTo(actual);
        verifyNoInteractions(comprobanteService);
    }

    @ParameterizedTest(name = "PENDIENTE -> {0}")
    @DisplayName("cambiarEstado: un pago pendiente solo puede aprobarse o rechazarse")
    @CsvSource({"PENDIENTE", "REEMBOLSADO"})
    void cambiarEstado_transicionNoPermitida(EstadoPago nuevo) {
        Pago pago = pagoPendiente("100.00");

        assertThatThrownBy(() -> service.cambiarEstado(7L, new CambioEstadoPagoRequest(nuevo)))
                .isInstanceOf(OperacionInvalidaException.class);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
    }

    @Test
    @DisplayName("cambiarEstado: un pago inexistente responde no encontrado")
    void cambiarEstado_noExiste() {
        when(pagoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(99L, new CambioEstadoPagoRequest(EstadoPago.APROBADO)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =====================  consultas  =====================

    @Test
    @DisplayName("listarMiosPorPedido: lista los pagos de un pedido propio")
    void listarMiosPorPedido_ok() {
        when(pedidoRepository.findByIdPedidoAndUsuario_Email(10L, EMAIL)).thenReturn(Optional.of(pedido));
        when(pagoRepository.findByPedido_IdPedidoOrderByIdPagoAsc(10L)).thenReturn(List.of(
                DatosDePrueba.pago(7L, pedido, "60.00", EstadoPago.APROBADO),
                DatosDePrueba.pago(8L, pedido, "40.00", EstadoPago.PENDIENTE)));

        List<PagoResponse> lista = service.listarMiosPorPedido(EMAIL, 10L);

        assertThat(lista).extracting(PagoResponse::idPago).containsExactly(7L, 8L);
    }

    @Test
    @DisplayName("listarMiosPorPedido: un pedido ajeno responde no encontrado sin consultar pagos")
    void listarMiosPorPedido_ajeno() {
        when(pedidoRepository.findByIdPedidoAndUsuario_Email(10L, EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarMiosPorPedido(EMAIL, 10L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(pagoRepository);
    }

    @Test
    @DisplayName("listarPorPedidoAdmin: verifica que el pedido exista")
    void listarPorPedidoAdmin() {
        when(pedidoRepository.existsById(10L)).thenReturn(true);
        when(pedidoRepository.existsById(99L)).thenReturn(false);
        when(pagoRepository.findByPedido_IdPedidoOrderByIdPagoAsc(10L)).thenReturn(List.of(
                DatosDePrueba.pago(7L, pedido, "100.00", EstadoPago.APROBADO)));

        assertThat(service.listarPorPedidoAdmin(10L)).hasSize(1);
        assertThatThrownBy(() -> service.listarPorPedidoAdmin(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("obtenerMio: un pago ajeno responde no encontrado")
    void obtenerMio() {
        Pago pago = DatosDePrueba.pago(7L, pedido, "100.00", EstadoPago.APROBADO);
        when(pagoRepository.findByIdPagoAndPedido_Usuario_Email(7L, EMAIL)).thenReturn(Optional.of(pago));
        when(pagoRepository.findByIdPagoAndPedido_Usuario_Email(8L, EMAIL)).thenReturn(Optional.empty());

        assertThat(service.obtenerMio(EMAIL, 7L).idPago()).isEqualTo(7L);
        assertThatThrownBy(() -> service.obtenerMio(EMAIL, 8L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("obtenerAdmin: devuelve el pago o responde no encontrado")
    void obtenerAdmin() {
        Pago pago = DatosDePrueba.pago(7L, pedido, "100.00", EstadoPago.PENDIENTE);
        when(pagoRepository.findById(7L)).thenReturn(Optional.of(pago));
        when(pagoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.obtenerAdmin(7L).estado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThatThrownBy(() -> service.obtenerAdmin(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
