package e.commerce.digital.mapper;

import e.commerce.digital.dto.subcategoria.SubcategoriaRequest;
import e.commerce.digital.dto.subcategoria.SubcategoriaResponse;
import e.commerce.digital.models.Categoria;
import e.commerce.digital.models.Subcategoria;
import org.springframework.stereotype.Component;

// La categoría la busca el service por req.idCategoria() y se la pasa ya cargada
@Component
public class SubcategoriaMapper {

    public Subcategoria toEntity(SubcategoriaRequest req, Categoria categoria) {
        Subcategoria s = new Subcategoria();
        s.setNombre(req.nombre());
        s.setDescripcion(req.descripcion());
        s.setActivo(req.activo() == null || req.activo());
        s.setCategoria(categoria);
        return s;
    }

    public void updateEntity(Subcategoria s, SubcategoriaRequest req, Categoria categoria) {
        s.setNombre(req.nombre());
        s.setDescripcion(req.descripcion());
        if (req.activo() != null) {
            s.setActivo(req.activo());
        }
        s.setCategoria(categoria);
    }

    public SubcategoriaResponse toResponse(Subcategoria s) {
        return SubcategoriaResponse.desde(s);
    }
}
