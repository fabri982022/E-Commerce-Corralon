package e.commerce.digital.dto.producto;

// Parámetros de búsqueda del catálogo (query params); todos opcionales
public record ProductoFiltroRequest(
        String texto,
        Long idCategoria,
        Long idSubcategoria
) {
}
