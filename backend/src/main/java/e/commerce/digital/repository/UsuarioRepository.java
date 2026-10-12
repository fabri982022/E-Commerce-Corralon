package e.commerce.digital.repository;

import e.commerce.digital.models.Usuario;
import e.commerce.digital.util.enumerated.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Login (Spring Security) y verificación de unicidad del email
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    // Listado de usuarios para el panel administrativo
    Page<Usuario> findByRol(Rol rol, Pageable pageable);
}
