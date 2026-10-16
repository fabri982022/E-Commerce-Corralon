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
import e.commerce.digital.repository.PagoRepository;
import e.commerce.digital.repository.PedidoRepository;
import e.commerce.digital.util.enumerated.EstadoPago;
import e.commerce.digital.util.enumerated.EstadoPedido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Un pago siempre nace PENDIENTE: el cliente informar que pagó nunca lo
 * aprueba.
 * Solo un administrador (o, a futuro, el proveedor de pagos) puede aprobar,
 * rechazar o reembolsar.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagoService {

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;
    private final PagoMapper pagoMapper;
    private final ComprobanteService comprobanteService;

    // ----- Registro -----

    /**
     * El cliente registra un pago sobre su propio pedido (por ejemplo, informa una
     * transferencia).
     */
    @Transactional
    public PagoResponse registrarComoCliente(String email, Long idPedido, PagoRequest req) {
        Pedido pedido = pedidoRepository.findByIdPedidoAndUsuario_Email(idPedido, email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
        return registrar(pedido, req);
    }

    /** El administrador registra un pago manual sobre cualquier pedido. */
    @Transactional
    public PagoResponse registrarComoAdmin(Long idPedido, PagoRequest req) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
        return registrar(pedido, req);
    }

    private PagoResponse registrar(Pedido pedido, PagoRequest req) {
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new OperacionInvalidaException("No se pueden registrar pagos de un pedido cancelado");
        }
        BigDecimal saldo = saldoPendiente(pedido);
        if (saldo.signum() <= 0) {
            throw new PagoYaProcesadoException("El pedido ya está pagado");
        }
        if (req.importe().compareTo(saldo) > 0) {
            throw new OperacionInvalidaException("El importe supera el saldo pendiente del pedido: " + saldo);
        }

        Pago pago = pagoMapper.toEntity(req); // estado PENDIENTE
        pedido.agregarPago(pago);
        return pagoMapper.toResponse(pagoRepository.save(pago));
    }

    // ----- Cambio de estado (administrador) -----

    /**
     * PENDIENTE pasa a APROBADO o RECHAZADO; APROBADO pasa a REEMBOLSADO. Cualquier
     * otro caso es
     * un pago ya procesado. Al aprobar: se registra la fecha, se emite el
     * comprobante interno y,
     * si los pagos aprobados cubren el total, el pedido pasa de PENDIENTE a
     * CONFIRMADO.
     * Un reembolso no repone stock ni cambia el estado del pedido: lo resuelve el
     * administrador.
     */
    @Transactional
    public PagoResponse cambiarEstado(Long idPago, CambioEstadoPagoRequest req) {
        Pago pago = buscar(idPago);
        EstadoPago actual = pago.getEstado();
        EstadoPago nuevo = req.estado();

        boolean procesado = actual == EstadoPago.RECHAZADO
                || actual == EstadoPago.REEMBOLSADO
                || (actual == EstadoPago.APROBADO && nuevo != EstadoPago.REEMBOLSADO);
        if (procesado) {
            throw new PagoYaProcesadoException("El pago ya fue procesado (estado actual: " + actual + ")");
        }
        // Desde acá, el estado actual es PENDIENTE (o APROBADO hacia REEMBOLSADO)
        if (actual == EstadoPago.PENDIENTE && nuevo != EstadoPago.APROBADO && nuevo != EstadoPago.RECHAZADO) {
            throw new OperacionInvalidaException("Un pago pendiente solo puede aprobarse o rechazarse");
        }

        switch (nuevo) {
            case APROBADO -> aprobar(pago);
            case RECHAZADO, REEMBOLSADO -> pago.setEstado(nuevo);
            default -> throw new OperacionInvalidaException("Transición de pago no permitida");
        }
        return pagoMapper.toResponse(pago);
    }

    private void aprobar(Pago pago) {
        Pedido pedido = pago.getPedido();
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new OperacionInvalidaException("No se puede aprobar el pago de un pedido cancelado");
        }
        BigDecimal aprobadoPrevio = aprobado(pedido);
        BigDecimal aprobadoTotal = aprobadoPrevio.add(pago.getImporte());
        if (aprobadoTotal.compareTo(pedido.getTotal()) > 0) {
            throw new OperacionInvalidaException("Los pagos aprobados superarían el total del pedido");
        }

        pago.setEstado(EstadoPago.APROBADO);
        pago.setFechaPago(LocalDateTime.now());
        comprobanteService.emitir(pago);

        if (aprobadoTotal.compareTo(pedido.getTotal()) == 0 && pedido.getEstado() == EstadoPedido.PENDIENTE) {
            pedido.setEstado(EstadoPedido.CONFIRMADO);
        }
    }

    // ----- Consultas -----

    public List<PagoResponse> listarMiosPorPedido(String email, Long idPedido) {
        pedidoRepository.findByIdPedidoAndUsuario_Email(idPedido, email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
        return listar(idPedido);
    }

    public List<PagoResponse> listarPorPedidoAdmin(Long idPedido) {
        if (!pedidoRepository.existsById(idPedido)) {
            throw new RecursoNoEncontradoException("Pedido no encontrado");
        }
        return listar(idPedido);
    }

    public PagoResponse obtenerMio(String email, Long idPago) {
        Pago pago = pagoRepository.findByIdPagoAndPedido_Usuario_Email(idPago, email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado"));
        return pagoMapper.toResponse(pago);
    }

    public PagoResponse obtenerAdmin(Long idPago) {
        return pagoMapper.toResponse(buscar(idPago));
    }

    // ----- Auxiliares -----

    private List<PagoResponse> listar(Long idPedido) {
        return pagoRepository.findByPedido_IdPedidoOrderByIdPagoAsc(idPedido).stream()
                .map(pagoMapper::toResponse)
                .toList();
    }

    private BigDecimal aprobado(Pedido pedido) {
        return pagoRepository.sumarImportePorPedidoYEstado(pedido.getIdPedido(), EstadoPago.APROBADO);
    }

    private BigDecimal saldoPendiente(Pedido pedido) {
        return pedido.getTotal().subtract(aprobado(pedido));
    }

    private Pago buscar(Long idPago) {
        return pagoRepository.findById(idPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado"));
    }
}