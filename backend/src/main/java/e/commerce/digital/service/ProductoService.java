package e.commerce.digital.service;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.producto.ProductoFiltroRequest;
import e.commerce.digital.dto.producto.ProductoRequest;
import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.ProductoMapper;
import e.commerce.digital.models.Producto;
import e.commerce.digital.models.Subcategoria;
import e.commerce.digital.repository.ProductoRepository;
import e.commerce.digital.repository.SubcategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final ProductoMapper productoMapper;

    // ----- Administración (la imagen se gestiona en ProductoImagenService) -----

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        Subcategoria subcategoria = buscarSubcategoria(req.idSubcategoria());
        Producto producto = productoMapper.toEntity(req, subcategoria);
        producto.setNombre(req.nombre().trim());
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    /** Actualiza datos, precio y existencias. */
    @Transactional
    public ProductoResponse actualizar(Long idProducto, ProductoRequest req) {
        Producto producto = buscar(idProducto);
        Subcategoria subcategoria = buscarSubcategoria(req.idSubcategoria());
        productoMapper.updateEntity(producto, req, subcategoria);
        producto.setNombre(req.nombre().trim());
        return productoMapper.toResponse(producto);
    }

    @Transactional
    public ProductoResponse cambiarEstado(Long idProducto, boolean activo) {
        Producto producto = buscar(idProducto);
        producto.setActivo(activo);
        return productoMapper.toResponse(producto);
    }

    public ProductoResponse obtenerAdmin(Long idProducto) {
        return productoMapper.toResponse(buscar(idProducto));
    }

    public PageResponse<ProductoResponse> buscarAdmin(ProductoFiltroRequest filtro, Pageable pageable) {
        return buscar(filtro, false, pageable);
    }

    // ----- Catálogo público -----

    /**
     * Un producto desactivado (o de una subcategoría o categoría inactiva) responde
     * como inexistente.
     */
    public ProductoResponse obtenerPublico(Long idProducto) {
        Producto producto = buscar(idProducto);
        if (!producto.estaDisponible()) {
            throw new RecursoNoEncontradoException("Producto no encontrado");
        }
        return productoMapper.toResponse(producto);
    }

    public PageResponse<ProductoResponse> buscarPublico(ProductoFiltroRequest filtro, Pageable pageable) {
        return buscar(filtro, true, pageable);
    }

    // ----- Auxiliares -----

    private PageResponse<ProductoResponse> buscar(ProductoFiltroRequest filtro, boolean soloActivos,
            Pageable pageable) {
        String texto = null;
        Long idCategoria = null;
        Long idSubcategoria = null;
        if (filtro != null) {
            texto = (filtro.texto() == null || filtro.texto().isBlank()) ? null : filtro.texto().trim();
            idCategoria = filtro.idCategoria();
            idSubcategoria = filtro.idSubcategoria();
        }
        Page<Producto> pagina = productoRepository.buscar(texto, idCategoria, idSubcategoria, soloActivos, pageable);
        return PageResponse.desde(pagina.map(productoMapper::toResponse));
    }

    private Producto buscar(Long idProducto) {
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }

    private Subcategoria buscarSubcategoria(Long idSubcategoria) {
        return subcategoriaRepository.findById(idSubcategoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Subcategoría no encontrada"));
    }
}