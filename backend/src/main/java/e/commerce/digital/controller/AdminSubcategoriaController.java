package e.commerce.digital.controller;

import e.commerce.digital.dto.subcategoria.SubcategoriaRequest;
import e.commerce.digital.dto.subcategoria.SubcategoriaResponse;
import e.commerce.digital.service.SubcategoriaService;
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

/** Gestión de subcategorías (RF04): cada una pertenece a una categoría. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSubcategoriaController {

    private final SubcategoriaService subcategoriaService;

    /** Incluye las inactivas. */
    @GetMapping("/categorias/{idCategoria}/subcategorias")
    public List<SubcategoriaResponse> listarPorCategoria(@PathVariable Long idCategoria) {
        return subcategoriaService.listarPorCategoria(idCategoria, false);
    }

    @GetMapping("/subcategorias/{idSubcategoria}")
    public SubcategoriaResponse obtener(@PathVariable Long idSubcategoria) {
        return subcategoriaService.obtener(idSubcategoria);
    }

    @PostMapping("/subcategorias")
    public ResponseEntity<SubcategoriaResponse> crear(@Valid @RequestBody SubcategoriaRequest req) {
        SubcategoriaResponse creada = subcategoriaService.crear(req);
        return Respuestas.creado(creada, "/api/admin/subcategorias/{id}", creada.idSubcategoria());
    }

    @PutMapping("/subcategorias/{idSubcategoria}")
    public SubcategoriaResponse actualizar(@PathVariable Long idSubcategoria,
                                           @Valid @RequestBody SubcategoriaRequest req) {
        return subcategoriaService.actualizar(idSubcategoria, req);
    }

    @PatchMapping("/subcategorias/{idSubcategoria}/activar")
    public SubcategoriaResponse activar(@PathVariable Long idSubcategoria) {
        return subcategoriaService.cambiarEstado(idSubcategoria, true);
    }

    @PatchMapping("/subcategorias/{idSubcategoria}/desactivar")
    public SubcategoriaResponse desactivar(@PathVariable Long idSubcategoria) {
        return subcategoriaService.cambiarEstado(idSubcategoria, false);
    }
}
