package e.commerce.digital.controller;

import e.commerce.digital.dto.categoria.CategoriaRequest;
import e.commerce.digital.dto.categoria.CategoriaResponse;
import e.commerce.digital.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/** Gestión de categorías (RF03). No se borran: se activan y desactivan. */
@RestController
@RequestMapping("/api/admin/categorias")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaService.listarTodas();
    }

    @GetMapping("/{idCategoria}")
    public CategoriaResponse obtener(@PathVariable Long idCategoria) {
        return categoriaService.obtener(idCategoria);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest req) {
        CategoriaResponse creada = categoriaService.crear(req);
        return Respuestas.creado(creada, "/api/admin/categorias/{id}", creada.idCategoria());
    }

    @PutMapping("/{idCategoria}")
    public CategoriaResponse actualizar(@PathVariable Long idCategoria, @Valid @RequestBody CategoriaRequest req) {
        return categoriaService.actualizar(idCategoria, req);
    }

    @PatchMapping("/{idCategoria}/activar")
    public CategoriaResponse activar(@PathVariable Long idCategoria) {
        return categoriaService.cambiarEstado(idCategoria, true);
    }

    @PatchMapping("/{idCategoria}/desactivar")
    public CategoriaResponse desactivar(@PathVariable Long idCategoria) {
        return categoriaService.cambiarEstado(idCategoria, false);
    }
}
