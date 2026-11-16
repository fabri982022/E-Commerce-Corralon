package e.commerce.digital.controller;

import e.commerce.digital.dto.categoria.CategoriaResponse;
import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.producto.ProductoFiltroRequest;
import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.dto.subcategoria.SubcategoriaResponse;
import e.commerce.digital.service.CategoriaService;
import e.commerce.digital.service.ProductoService;
import e.commerce.digital.service.SubcategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catálogo público (RF06): no requiere iniciar sesión y solo muestra lo disponible. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogoController {

    private final CategoriaService categoriaService;
    private final SubcategoriaService subcategoriaService;
    private final ProductoService productoService;

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return categoriaService.listarActivas();
    }

    @GetMapping("/categorias/{idCategoria}/subcategorias")
    public List<SubcategoriaResponse> subcategorias(@PathVariable Long idCategoria) {
        return subcategoriaService.listarPorCategoria(idCategoria, true);
    }

    /** Parámetros opcionales: texto, idCategoria, idSubcategoria, page, size y sort. */
    @GetMapping("/productos")
    public PageResponse<ProductoResponse> productos(
            ProductoFiltroRequest filtro,
            @PageableDefault(size = 12, sort = "nombre") Pageable pageable) {
        return productoService.buscarPublico(filtro, pageable);
    }

    @GetMapping("/productos/{idProducto}")
    public ProductoResponse producto(@PathVariable Long idProducto) {
        return productoService.obtenerPublico(idProducto);
    }
}
