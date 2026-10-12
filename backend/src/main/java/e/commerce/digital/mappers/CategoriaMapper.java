package e.commerce.digital.mappers;

import e.commerce.digital.dto.categoria.CategoriaRequest;
import e.commerce.digital.dto.categoria.CategoriaResponse;
import e.commerce.digital.models.Categoria;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

    public Categoria toEntity(CategoriaRequest req) {
        Categoria c = new Categoria();
        c.setNombre(req.nombre());
        c.setDescripcion(req.descripcion());
        c.setActivo(req.activo() == null || req.activo());
        return c;
    }

    public void updateEntity(Categoria c, CategoriaRequest req) {
        c.setNombre(req.nombre());
        c.setDescripcion(req.descripcion());
        if (req.activo() != null) {
            c.setActivo(req.activo());
        }
    }

    public CategoriaResponse toResponse(Categoria c) {
        return CategoriaResponse.desde(c);
    }
}