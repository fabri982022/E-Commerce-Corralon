package e.commerce.digital.controller;

import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.dto.producto.ProductoVendedorRequest;
import e.commerce.digital.service.ProductoImagenService;
import e.commerce.digital.service.VendedorProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/vendedor/productos")
@PreAuthorize("hasRole('VENDEDOR')")
@RequiredArgsConstructor
public class VendedorProductoController {

    private final VendedorProductoService vendedorProductoService;
    private final ProductoImagenService productoImagenService;

    @GetMapping
    public List<ProductoResponse> listar(Authentication authentication) {
        return vendedorProductoService.listar(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(
            Authentication authentication,
            @Valid @RequestBody ProductoVendedorRequest req) {
        ProductoResponse creado = vendedorProductoService.crear(authentication.getName(), req);
        return Respuestas.creado(creado, "/api/vendedor/productos/{id}", creado.idProducto());
    }

    @PutMapping("/{idProducto}")
    public ProductoResponse actualizar(
            Authentication authentication,
            @PathVariable Long idProducto,
            @Valid @RequestBody ProductoVendedorRequest req) {
        return vendedorProductoService.actualizar(authentication.getName(), idProducto, req);
    }

    @PutMapping(value = "/{idProducto}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductoResponse subirImagen(
            Authentication authentication,
            @PathVariable Long idProducto,
            @RequestPart("archivo") MultipartFile archivo) {
        return productoImagenService.actualizarImagenVendedor(authentication.getName(), idProducto, archivo);
    }
}
