package e.commerce.digital.controller;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.comprobante.ComprobanteResponse;
import e.commerce.digital.service.ComprobanteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de todos los comprobantes del negocio (RF12). */
@RestController
@RequestMapping("/api/admin/comprobantes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminComprobanteController {

    private final ComprobanteService comprobanteService;

    @GetMapping
    public PageResponse<ComprobanteResponse> listar(
            @PageableDefault(size = 20, sort = "fechaEmision", direction = Sort.Direction.DESC) Pageable pageable) {
        return comprobanteService.listarTodos(pageable);
    }

    @GetMapping("/{idComprobante}")
    public ComprobanteResponse obtener(@PathVariable Long idComprobante) {
        return comprobanteService.obtenerAdmin(idComprobante);
    }
}
