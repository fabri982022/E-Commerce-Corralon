package e.commerce.digital.repository;

import e.commerce.digital.models.Pago;
import e.commerce.digital.util.enumerated.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByPedido_IdPedidoOrderByIdPagoAsc(Long idPedido);

    // Pago de un pedido del cliente autenticado
    Optional<Pago> findByIdPagoAndPedido_Usuario_Email(Long idPago, String email);

    // "Pago ya procesado": evita aprobar dos veces o duplicar un pago aprobado
    boolean existsByPedido_IdPedidoAndEstado(Long idPedido, EstadoPago estado);

    // Total aprobado de un pedido, para saber si está completamente pagado (pagos parciales)
    @Query("""
            SELECT COALESCE(SUM(p.importe), 0) FROM Pago p
            WHERE p.pedido.idPedido = :idPedido AND p.estado = :estado
            """)
    BigDecimal sumarImportePorPedidoYEstado(@Param("idPedido") Long idPedido,
                                            @Param("estado") EstadoPago estado);
}
