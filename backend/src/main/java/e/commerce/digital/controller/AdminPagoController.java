package e.commerce.digital.controller;

import e.commerce.digital.dto.pago.CambioEstadoPagoRequest;
import e.commerce.digital.dto.pago.PagoResponse;
import e.commerce.digital.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Verificación de pagos (RF11): aprobar, rechazar o reembolsar. Aprobar emite el comprobante. */
@RestController
@RequestMapping("/api/admin/pagos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPagoController {

    private final PagoService pagoService;

    @GetMapping("/{idPago}")
    public PagoResponse obtener(@PathVariable Long idPago) {
        return pagoService.obtenerAdmin(idPago);
    }

    @PatchMapping("/{idPago}/estado")
    public PagoResponse cambiarEstado(@PathVariable Long idPago,
                                      @Valid @RequestBody CambioEstadoPagoRequest req) {
        return pagoService.cambiarEstado(idPago, req);
    }
}
