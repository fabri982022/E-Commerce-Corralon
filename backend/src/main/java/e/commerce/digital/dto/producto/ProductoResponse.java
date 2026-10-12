package e.commerce.digital.dto.producto;

import e.commerce.digital.models.Producto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long idProducto,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        String imagenUrl,
        Boolean activo,
        Long idSubcategoria,
        String nombreSubcategoria,
        Long idCategoria,
        String nombreCategoria
) {
    public static ProductoResponse desde(Producto p) {
        var sub = p.getSubcategoria();
        var cat = sub.getCategoria();
        return new ProductoResponse(
                p.getIdProducto(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getStock(),
                p.getImagenUrl(),
                p.getActivo(),
                sub.getIdSubcategoria(),
                sub.getNombre(),
                cat.getIdCategoria(),
                cat.getNombre());
    }
}
