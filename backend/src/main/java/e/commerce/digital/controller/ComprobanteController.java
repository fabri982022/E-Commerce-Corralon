package e.commerce.digital.controller;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.comprobante.ComprobanteResponse;
import e.commerce.digital.service.ComprobanteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Comprobantes internos del cliente autenticado (solo lectura: los genera el servidor). */
@RestController
@RequestMapping("/api/comprobantes")
@PreAuthorize("hasRole('CLIENTE')")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    @GetMapping
    public PageResponse<ComprobanteResponse> listar(
            Authentication auth,
            @PageableDefault(size = 10, sort = "fechaEmision", direction = Sort.Direction.DESC) Pageable pageable) {
        return comprobanteService.listarMios(auth.getName(), pageable);
    }

    @GetMapping("/{idComprobante}")
    public ComprobanteResponse obtener(@PathVariable Long idComprobante, Authentication auth) {
        return comprobanteService.obtenerMio(auth.getName(), idComprobante);
    }
}
