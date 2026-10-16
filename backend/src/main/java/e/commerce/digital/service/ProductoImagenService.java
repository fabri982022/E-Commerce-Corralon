package e.commerce.digital.service;

import e.commerce.digital.dto.common.ImagenResponse;
import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.mapper.ProductoMapper;
import e.commerce.digital.models.Producto;
import e.commerce.digital.repository.ProductoRepository;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductoImagenService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final ImagenService imagenService;

    /** Sube o reemplaza la imagen de un producto (solo administrador). */
    @Transactional
    public ProductoResponse actualizarImagen(Long idProducto, MultipartFile archivo) {
        Producto producto = buscar(idProducto);
        String publicIdAnterior = producto.getImagenPublicId();

        ImagenResponse nueva = imagenService.subir(archivo, ImagenService.CARPETA_PRODUCTOS);
        producto.setImagenUrl(nueva.url());
        producto.setImagenPublicId(nueva.publicId());

        imagenService.limpiarAlFinalizarTransaccion(publicIdAnterior, nueva.publicId());
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse eliminarImagen(Long idProducto) {
        Producto producto = buscar(idProducto);
        String publicIdAnterior = producto.getImagenPublicId();

        producto.setImagenUrl(null);
        producto.setImagenPublicId(null);

        imagenService.limpiarAlFinalizarTransaccion(publicIdAnterior, null);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    private Producto buscar(Long idProducto) {
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }
}
