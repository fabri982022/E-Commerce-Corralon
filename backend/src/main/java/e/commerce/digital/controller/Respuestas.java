package e.commerce.digital.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Utilidad común de los controladores. */
final class Respuestas {

    private Respuestas() {
    }

    /**
     * 201 Created con el encabezado Location apuntando al recurso nuevo.
     * Ejemplo: creado(producto, "/api/admin/productos/{id}", producto.idProducto())
     */
    static <T> ResponseEntity<T> creado(T cuerpo, String rutaDelRecurso, Object id) {
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(rutaDelRecurso)
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(ubicacion).body(cuerpo);
    }
}
