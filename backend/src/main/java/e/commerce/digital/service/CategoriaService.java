package e.commerce.digital.service;

import e.commerce.digital.dto.categoria.CategoriaRequest;
import e.commerce.digital.dto.categoria.CategoriaResponse;
import e.commerce.digital.exception.ConflictoException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.CategoriaMapper;
import e.commerce.digital.models.Categoria;
import e.commerce.digital.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional
    public CategoriaResponse crear(CategoriaRequest req) {
        String nombre = req.nombre().trim();
        if (categoriaRepository.existsByNombre(nombre)) {
            throw new ConflictoException("Ya existe una categoría con ese nombre");
        }
        Categoria categoria = categoriaMapper.toEntity(req);
        categoria.setNombre(nombre);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long idCategoria, CategoriaRequest req) {
        Categoria categoria = buscar(idCategoria);
        String nombre = req.nombre().trim();
        if (categoriaRepository.existsByNombreAndIdCategoriaNot(nombre, idCategoria)) {
            throw new ConflictoException("Ya existe una categoría con ese nombre");
        }
        categoriaMapper.updateEntity(categoria, req);
        categoria.setNombre(nombre);
        return categoriaMapper.toResponse(categoria);
    }

    /**
     * Activar o desactivar. Los productos de una categoría inactiva dejan de
     * aparecer en el catálogo público.
     */
    @Transactional
    public CategoriaResponse cambiarEstado(Long idCategoria, boolean activo) {
        Categoria categoria = buscar(idCategoria);
        categoria.setActivo(activo);
        return categoriaMapper.toResponse(categoria);
    }

    public CategoriaResponse obtener(Long idCategoria) {
        return categoriaMapper.toResponse(buscar(idCategoria));
    }

    /** Catálogo público: solo categorías activas. */
    public List<CategoriaResponse> listarActivas() {
        return categoriaRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(categoriaMapper::toResponse)
                .toList();
    }

    /** Panel administrativo: todas, activas e inactivas. */
    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAll(Sort.by("nombre")).stream()
                .map(categoriaMapper::toResponse)
                .toList();
    }

    private Categoria buscar(Long idCategoria) {
        return categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));
    }
}