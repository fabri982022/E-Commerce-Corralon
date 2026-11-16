package e.commerce.digital.controller;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.producto.ProductoFiltroRequest;
import e.commerce.digital.dto.producto.ProductoRequest;
import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de productos (RF05): precio, existencias y estado. La imagen se sube
 * en ProductoImagenAdminController (/api/admin/productos/{id}/imagen).
 */
@RestController
@RequestMapping("/api/admin/productos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductoController {

    private final ProductoService productoService;

    /** Incluye los productos inactivos. */
    @GetMapping
    public PageResponse<ProductoResponse> listar(
            ProductoFiltroRequest filtro,
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return productoService.buscarAdmin(filtro, pageable);
    }

    @GetMapping("/{idProducto}")
    public ProductoResponse obtener(@PathVariable Long idProducto) {
        return productoService.obtenerAdmin(idProducto);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        ProductoResponse creado = productoService.crear(req);
        return Respuestas.creado(creado, "/api/admin/productos/{id}", creado.idProducto());
    }

    @PutMapping("/{idProducto}")
    public ProductoResponse actualizar(@PathVariable Long idProducto, @Valid @RequestBody ProductoRequest req) {
        return productoService.actualizar(idProducto, req);
    }

    @PatchMapping("/{idProducto}/activar")
    public ProductoResponse activar(@PathVariable Long idProducto) {
        return productoService.cambiarEstado(idProducto, true);
    }

    @PatchMapping("/{idProducto}/desactivar")
    public ProductoResponse desactivar(@PathVariable Long idProducto) {
        return productoService.cambiarEstado(idProducto, false);
    }
}
