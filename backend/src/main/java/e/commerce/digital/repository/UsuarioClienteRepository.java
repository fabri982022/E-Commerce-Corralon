package e.commerce.digital.repository;

import e.commerce.digital.models.UsuarioCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Para operaciones que solo tienen sentido sobre clientes (carrito, pedidos)
public interface UsuarioClienteRepository extends JpaRepository<UsuarioCliente, Long> {

    Optional<UsuarioCliente> findByEmail(String email);
}
