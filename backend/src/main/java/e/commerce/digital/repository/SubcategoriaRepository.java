package e.commerce.digital.repository;

import e.commerce.digital.models.Subcategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Long> {

    // Panel administrativo: todas las subcategorías de una categoría
    List<Subcategoria> findByCategoria_IdCategoriaOrderByNombreAsc(Long idCategoria);

    // Catálogo público: solo las activas
    List<Subcategoria> findByCategoria_IdCategoriaAndActivoTrueOrderByNombreAsc(Long idCategoria);

    // Evita repetir el nombre dentro de la misma categoría
    boolean existsByNombreAndCategoria_IdCategoria(String nombre, Long idCategoria);

    boolean existsByNombreAndCategoria_IdCategoriaAndIdSubcategoriaNot(
            String nombre, Long idCategoria, Long idSubcategoria);
}
