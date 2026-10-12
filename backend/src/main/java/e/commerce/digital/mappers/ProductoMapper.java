package e.commerce.digital.mappers;

import e.commerce.digital.dto.producto.ProductoRequest;
import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.models.Producto;
import org.springframework.stereotype.Component;

// La subcategoría la busca el service por req.idSubcategoria() y se la pasa ya cargada
@Component
public class ProductoMapper {

    public Producto toEntity(ProductoRequest req, Subcategorias subcategoria) {
        Producto p = new Producto();
        p.setNombre(req.nombre());
        p.setDescripcion(req.descripcion());
        p.setPrecio(req.precio());
        p.setStock(req.stock());
        p.setImagenUrl(req.imagenUrl());
        p.setActivo(req.activo() == null || req.activo());
        p.setSubcategoria(subcategoria);
        return p;
    }

    public void updateEntity(Producto p, ProductoRequest req, Subcategorias subcategoria) {
        p.setNombre(req.nombre());
        p.setDescripcion(req.descripcion());
        p.setPrecio(req.precio());
        p.setStock(req.stock());
        p.setImagenUrl(req.imagenUrl());
        if (req.activo() != null) {
            p.setActivo(req.activo());
        }
        p.setSubcategoria(subcategoria);
    }

    public ProductoResponse toResponse(Producto p) {
        return ProductoResponse.desde(p);
    }
}