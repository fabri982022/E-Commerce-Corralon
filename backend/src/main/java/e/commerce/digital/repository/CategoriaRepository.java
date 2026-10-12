package e.commerce.digital.repository;

import e.commerce.digital.models.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Validación de nombre único al crear y al editar
    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdCategoriaNot(String nombre, Long idCategoria);

    // Catálogo público: solo categorías activas
    List<Categoria> findByActivoTrueOrderByNombreAsc();
}
