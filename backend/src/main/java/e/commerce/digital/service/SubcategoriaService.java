package e.commerce.digital.service;

import e.commerce.digital.dto.subcategoria.SubcategoriaRequest;
import e.commerce.digital.dto.subcategoria.SubcategoriaResponse;
import e.commerce.digital.exception.ConflictoException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.SubcategoriaMapper;
import e.commerce.digital.models.Categoria;
import e.commerce.digital.models.Subcategoria;
import e.commerce.digital.repository.CategoriaRepository;
import e.commerce.digital.repository.SubcategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubcategoriaService {

    private final SubcategoriaRepository subcategoriaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaMapper subcategoriaMapper;

    @Transactional
    public SubcategoriaResponse crear(SubcategoriaRequest req) {
        Categoria categoria = buscarCategoria(req.idCategoria());
        String nombre = req.nombre().trim();
        if (subcategoriaRepository.existsByNombreAndCategoria_IdCategoria(nombre, req.idCategoria())) {
            throw new ConflictoException("Ya existe una subcategoría con ese nombre en la categoría");
        }
        Subcategoria subcategoria = subcategoriaMapper.toEntity(req, categoria);
        subcategoria.setNombre(nombre);
        return subcategoriaMapper.toResponse(subcategoriaRepository.save(subcategoria));
    }

    @Transactional
    public SubcategoriaResponse actualizar(Long idSubcategoria, SubcategoriaRequest req) {
        Subcategoria subcategoria = buscar(idSubcategoria);
        Categoria categoria = buscarCategoria(req.idCategoria());
        String nombre = req.nombre().trim();
        if (subcategoriaRepository.existsByNombreAndCategoria_IdCategoriaAndIdSubcategoriaNot(
                nombre, req.idCategoria(), idSubcategoria)) {
            throw new ConflictoException("Ya existe una subcategoría con ese nombre en la categoría");
        }
        subcategoriaMapper.updateEntity(subcategoria, req, categoria);
        subcategoria.setNombre(nombre);
        return subcategoriaMapper.toResponse(subcategoria);
    }

    @Transactional
    public SubcategoriaResponse cambiarEstado(Long idSubcategoria, boolean activo) {
        Subcategoria subcategoria = buscar(idSubcategoria);
        subcategoria.setActivo(activo);
        return subcategoriaMapper.toResponse(subcategoria);
    }

    public SubcategoriaResponse obtener(Long idSubcategoria) {
        return subcategoriaMapper.toResponse(buscar(idSubcategoria));
    }

    /**
     * soloActivas = true para el catálogo público; false para el panel
     * administrativo.
     */
    public List<SubcategoriaResponse> listarPorCategoria(Long idCategoria, boolean soloActivas) {
        buscarCategoria(idCategoria); // 404 si la categoría no existe
        List<Subcategoria> lista = soloActivas
                ? subcategoriaRepository.findByCategoria_IdCategoriaAndActivoTrueOrderByNombreAsc(idCategoria)
                : subcategoriaRepository.findByCategoria_IdCategoriaOrderByNombreAsc(idCategoria);
        return lista.stream().map(subcategoriaMapper::toResponse).toList();
    }

    private Subcategoria buscar(Long idSubcategoria) {
        return subcategoriaRepository.findById(idSubcategoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Subcategoría no encontrada"));
    }

    private Categoria buscarCategoria(Long idCategoria) {
        return categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));
    }
}