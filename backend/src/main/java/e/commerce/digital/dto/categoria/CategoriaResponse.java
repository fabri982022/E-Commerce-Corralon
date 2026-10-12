package e.commerce.digital.dto.categoria;

import e.commerce.digital.models.Categoria;

public record CategoriaResponse(
        Long idCategoria,
        String nombre,
        String descripcion,
        Boolean activo
) {
    public static CategoriaResponse desde(Categoria c) {
        return new CategoriaResponse(
                c.getIdCategoria(),
                c.getNombre(),
                c.getDescripcion(),
                c.getActivo());
    }
}
