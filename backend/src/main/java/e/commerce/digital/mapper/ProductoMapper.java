package e.commerce.digital.mapper;

import e.commerce.digital.dto.producto.ProductoRequest;
import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.models.Producto;
import e.commerce.digital.models.Subcategoria;
import org.springframework.stereotype.Component;

// La subcategoría la busca el service por req.idSubcategoria() y se la pasa ya cargada
@Component
public class ProductoMapper {

    public Producto toEntity(ProductoRequest req, Subcategoria subcategoria) {
        Producto p = new Producto();
        p.setNombre(req.nombre());
        p.setDescripcion(req.descripcion());
        p.setPrecio(req.precio());
        p.setStock(req.stock());
        p.setActivo(req.activo() == null || req.activo());
        p.setSubcategoria(subcategoria);
        return p;
    }

    public void updateEntity(Producto p, ProductoRequest req, Subcategoria subcategoria) {
        p.setNombre(req.nombre());
        p.setDescripcion(req.descripcion());
        p.setPrecio(req.precio());
        p.setStock(req.stock());
        if (req.activo() != null) {
            p.setActivo(req.activo());
        }
        p.setSubcategoria(subcategoria);
    }

    public ProductoResponse toResponse(Producto p) {
        return ProductoResponse.desde(p);
    }
}
