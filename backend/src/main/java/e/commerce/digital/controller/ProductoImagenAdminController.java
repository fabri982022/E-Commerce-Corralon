package e.commerce.digital.controller;

import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.service.ProductoImagenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/productos/{idProducto}/imagen")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ProductoImagenAdminController {

    private final ProductoImagenService productoImagenService;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductoResponse subir(@PathVariable Long idProducto,
                                  @RequestPart("archivo") MultipartFile archivo) {
        return productoImagenService.actualizarImagen(idProducto, archivo);
    }

    @DeleteMapping
    public ProductoResponse eliminar(@PathVariable Long idProducto) {
        return productoImagenService.eliminarImagen(idProducto);
    }
}
