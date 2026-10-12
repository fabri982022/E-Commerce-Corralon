package e.commerce.digital.dto.subcategoria;

import e.commerce.digital.models.Subcategoria;

public record SubcategoriaResponse(
        Long idSubcategoria,
        String nombre,
        String descripcion,
        Boolean activo,
        Long idCategoria,
        String nombreCategoria
) {
    public static SubcategoriaResponse desde(Subcategoria s) {
        return new SubcategoriaResponse(
                s.getIdSubcategoria(),
                s.getNombre(),
                s.getDescripcion(),
                s.getActivo(),
                s.getCategoria().getIdCategoria(),
                s.getCategoria().getNombre());
    }
}
