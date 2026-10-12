package e.commerce.digital.repository;

import e.commerce.digital.models.Pedido;
import e.commerce.digital.util.enumerated.EstadoPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Historial del cliente autenticado (paginado)
    @EntityGraph(attributePaths = "usuario")
    Page<Pedido> findByUsuario_Email(String email, Pageable pageable);

    // Detalle de un pedido del cliente: la condición del email impide ver pedidos ajenos.
    // Se cargan detalles y productos; pagos queda lazy (el service es @Transactional).
    @EntityGraph(attributePaths = {"usuario", "detalles", "detalles.producto"})
    Optional<Pedido> findByIdPedidoAndUsuario_Email(Long idPedido, String email);

    // Detalle para el administrador
    @EntityGraph(attributePaths = {"usuario", "detalles", "detalles.producto"})
    Optional<Pedido> findWithDetallesByIdPedido(Long idPedido);

    /**
     * Filtros del administrador (todos opcionales).
     * desde es inclusivo y hasta es exclusivo: el service convierte las fechas con
     * desde.atStartOfDay() y hasta.plusDays(1).atStartOfDay().
     */
    @EntityGraph(attributePaths = "usuario")
    @Query("""
            SELECT p FROM Pedido p
            WHERE (:estado IS NULL OR p.estado = :estado)
              AND (:idUsuario IS NULL OR p.usuario.idUsuario = :idUsuario)
              AND (:desde IS NULL OR p.fechaPedido >= :desde)
              AND (:hasta IS NULL OR p.fechaPedido < :hasta)
            """)
    Page<Pedido> filtrar(@Param("estado") EstadoPedido estado,
                         @Param("idUsuario") Long idUsuario,
                         @Param("desde") LocalDateTime desde,
                         @Param("hasta") LocalDateTime hasta,
                         Pageable pageable);
}
