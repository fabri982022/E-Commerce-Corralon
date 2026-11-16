package e.commerce.digital.controller;

import e.commerce.digital.dto.carrito.CarritoItemCantidadRequest;
import e.commerce.digital.dto.carrito.CarritoItemRequest;
import e.commerce.digital.dto.carrito.CarritoResponse;
import e.commerce.digital.service.CarritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Carrito del cliente autenticado: nunca se recibe un id de usuario, se usa el de la sesión. */
@RestController
@RequestMapping("/api/carrito")
@PreAuthorize("hasRole('CLIENTE')")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping
    public CarritoResponse obtener(Authentication auth) {
        return carritoService.obtener(auth.getName());
    }

    @PostMapping("/items")
    public CarritoResponse agregar(@Valid @RequestBody CarritoItemRequest req, Authentication auth) {
        return carritoService.agregarProducto(auth.getName(), req);
    }

    @PutMapping("/items/{idItem}")
    public CarritoResponse cambiarCantidad(@PathVariable Long idItem,
                                           @Valid @RequestBody CarritoItemCantidadRequest req,
                                           Authentication auth) {
        return carritoService.actualizarCantidad(auth.getName(), idItem, req);
    }

    @DeleteMapping("/items/{idItem}")
    public CarritoResponse quitar(@PathVariable Long idItem, Authentication auth) {
        return carritoService.quitarItem(auth.getName(), idItem);
    }

    @DeleteMapping
    public CarritoResponse vaciar(Authentication auth) {
        return carritoService.vaciar(auth.getName());
    }
}
