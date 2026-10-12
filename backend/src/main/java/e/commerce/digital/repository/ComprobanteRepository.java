package e.commerce.digital.repository;

import e.commerce.digital.models.Comprobante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    Optional<Comprobante> findByPago_IdPago(Long idPago);

    // Comprobantes del cliente autenticado
    Page<Comprobante> findByPago_Pedido_Usuario_Email(String email, Pageable pageable);

    Optional<Comprobante> findByIdComprobanteAndPago_Pedido_Usuario_Email(Long idComprobante, String email);

    // Para validar el número generado antes de guardar
    boolean existsByNumero(String numero);
}
