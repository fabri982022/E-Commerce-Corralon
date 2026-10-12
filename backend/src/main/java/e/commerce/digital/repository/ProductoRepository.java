package e.commerce.digital.repository;

import e.commerce.digital.models.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Búsqueda paginada con filtros opcionales (texto, categoría, subcategoría).
     * soloActivos = true  -> catálogo público: producto, subcategoría y categoría activos.
     * soloActivos = false -> panel administrativo: incluye inactivos.
     */
    @Query("""
            SELECT p FROM Producto p
            JOIN p.subcategoria s
            JOIN s.categoria c
            WHERE (:soloActivos = false
                   OR (p.activo = true AND s.activo = true AND c.activo = true))
              AND (:texto IS NULL
                   OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:idCategoria IS NULL OR c.idCategoria = :idCategoria)
              AND (:idSubcategoria IS NULL OR s.idSubcategoria = :idSubcategoria)
            """)
    Page<Producto> buscar(@Param("texto") String texto,
                          @Param("idCategoria") Long idCategoria,
                          @Param("idSubcategoria") Long idSubcategoria,
                          @Param("soloActivos") boolean soloActivos,
                          Pageable pageable);

    // Para impedir borrar o desactivar subcategorías con productos asociados
    boolean existsBySubcategoria_IdSubcategoria(Long idSubcategoria);

    /**
     * Opción A para stock: bloqueo pesimista al confirmar un pedido.
     * Se piden todos los productos juntos y ordenados por id para evitar deadlocks.
     * Debe llamarse dentro de una transacción.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto IN :ids ORDER BY p.idProducto")
    List<Producto> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    /**
     * Opción B para stock: descuento atómico en una sola sentencia.
     * Devuelve 1 si había stock suficiente y 0 si no (el service lanza StockInsuficienteException).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Producto p SET p.stock = p.stock - :cantidad
            WHERE p.idProducto = :id AND p.stock >= :cantidad AND p.activo = true
            """)
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);

    // Reposición al cancelar un pedido
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock + :cantidad WHERE p.idProducto = :id")
    int reponerStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
