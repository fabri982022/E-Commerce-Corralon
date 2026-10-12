package e.commerce.digital.repository;

import e.commerce.digital.models.Carrito;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    // Carrito del usuario autenticado, con items y productos en una sola consulta
    @EntityGraph(attributePaths = {"items", "items.producto"})
    Optional<Carrito> findByUsuario_Email(String email);

    Optional<Carrito> findByUsuario_IdUsuario(Long idUsuario);

    boolean existsByUsuario_IdUsuario(Long idUsuario);
}
