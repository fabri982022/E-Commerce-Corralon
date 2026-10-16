package e.commerce.digital.service;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.comprobante.ComprobanteResponse;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.ComprobanteMapper;
import e.commerce.digital.models.Comprobante;
import e.commerce.digital.models.Pago;
import e.commerce.digital.repository.ComprobanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComprobanteService {

    // Comprobante interno: no es una factura fiscal
    static final String TIPO_INTERNO = "COMPROBANTE_INTERNO";

    private final ComprobanteRepository comprobanteRepository;
    private final ComprobanteMapper comprobanteMapper;

    /**
     * Emite el comprobante de un pago aprobado. Lo llama PagoService dentro de su
     * transacción.
     * El número se deriva del id del pago (único, sin carreras y sin huecos en la
     * numeración).
     * Si el pago ya tiene comprobante, lo devuelve sin crear otro.
     */
    @Transactional
    public Comprobante emitir(Pago pago) {
        if (pago.getComprobante() != null) {
            return pago.getComprobante();
        }
        String numero = String.format("COMP-%08d", pago.getIdPago());
        Comprobante comprobante = comprobanteMapper.toEntity(pago, numero, TIPO_INTERNO, null);
        pago.setComprobante(comprobante);
        return comprobanteRepository.save(comprobante);
    }

    // ----- Cliente -----

    public PageResponse<ComprobanteResponse> listarMios(String email, Pageable pageable) {
        return PageResponse.desde(comprobanteRepository.findByPago_Pedido_Usuario_Email(email, pageable)
                .map(comprobanteMapper::toResponse));
    }

    public ComprobanteResponse obtenerMio(String email, Long idComprobante) {
        Comprobante comprobante = comprobanteRepository
                .findByIdComprobanteAndPago_Pedido_Usuario_Email(idComprobante, email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comprobante no encontrado"));
        return comprobanteMapper.toResponse(comprobante);
    }

    // ----- Administración -----

    public PageResponse<ComprobanteResponse> listarTodos(Pageable pageable) {
        return PageResponse.desde(comprobanteRepository.findAll(pageable).map(comprobanteMapper::toResponse));
    }

    public ComprobanteResponse obtenerAdmin(Long idComprobante) {
        return comprobanteMapper.toResponse(comprobanteRepository.findById(idComprobante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comprobante no encontrado")));
    }
}