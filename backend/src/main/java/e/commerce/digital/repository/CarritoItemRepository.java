package e.commerce.digital.repository;

import e.commerce.digital.models.CarritoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> {

    // Para sumar cantidad si el producto ya está en el carrito
    Optional<CarritoItem> findByCarrito_IdCarritoAndProducto_IdProducto(Long idCarrito, Long idProducto);

    // Garantiza que el item pertenece al carrito del usuario autenticado (modificar o eliminar)
    Optional<CarritoItem> findByIdCarritoItemAndCarrito_Usuario_Email(Long idCarritoItem, String email);
}
