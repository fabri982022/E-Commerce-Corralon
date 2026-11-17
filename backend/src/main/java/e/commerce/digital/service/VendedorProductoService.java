package e.commerce.digital.service;

import e.commerce.digital.dto.producto.ProductoRequest;
import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.dto.producto.ProductoVendedorRequest;
import e.commerce.digital.exception.OperacionInvalidaException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.ProductoMapper;
import e.commerce.digital.models.Producto;
import e.commerce.digital.models.Subcategoria;
import e.commerce.digital.models.Usuario;
import e.commerce.digital.models.UsuarioVendedor;
import e.commerce.digital.repository.ProductoRepository;
import e.commerce.digital.repository.SubcategoriaRepository;
import e.commerce.digital.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendedorProductoService {

    private final ProductoRepository productoRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoMapper productoMapper;

    public List<ProductoResponse> listar(String email) {
        UsuarioVendedor vendedor = buscarVendedor(email);
        return productoRepository.findByVendedor_IdUsuarioOrderByIdProductoDesc(vendedor.getIdUsuario())
                .stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductoResponse crear(String email, ProductoVendedorRequest req) {
        UsuarioVendedor vendedor = buscarVendedor(email);
        Subcategoria subcategoria = buscarSubcategoriaActiva(req.idSubcategoria());
        Producto producto = productoMapper.toEntity(requestSinEstado(req), subcategoria);
        producto.setNombre(req.nombre().trim());
        producto.setVendedor(vendedor);
        producto.setActivo(false);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(String email, Long idProducto, ProductoVendedorRequest req) {
        UsuarioVendedor vendedor = buscarVendedor(email);
        Producto producto = productoRepository.findByIdProductoAndVendedor_Email(idProducto, vendedor.getEmail())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
        Subcategoria subcategoria = buscarSubcategoriaActiva(req.idSubcategoria());
        productoMapper.updateEntity(producto, requestSinEstado(req), subcategoria);
        producto.setNombre(req.nombre().trim());
        producto.setActivo(false);
        return productoMapper.toResponse(producto);
    }

    private UsuarioVendedor buscarVendedor(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta vendedora no encontrada"));
        if (!(usuario instanceof UsuarioVendedor vendedor)
                || !Boolean.TRUE.equals(vendedor.getActivo())) {
            throw new OperacionInvalidaException("La cuenta vendedora no está habilitada");
        }
        return vendedor;
    }

    private Subcategoria buscarSubcategoriaActiva(Long idSubcategoria) {
        Subcategoria subcategoria = subcategoriaRepository.findById(idSubcategoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Subcategoría no encontrada"));
        if (!Boolean.TRUE.equals(subcategoria.getActivo())
                || !Boolean.TRUE.equals(subcategoria.getCategoria().getActivo())) {
            throw new OperacionInvalidaException("La categoría seleccionada no está disponible");
        }
        return subcategoria;
    }

    private ProductoRequest requestSinEstado(ProductoVendedorRequest req) {
        return new ProductoRequest(req.nombre(), req.descripcion(), req.precio(), req.stock(), false,
                req.idSubcategoria());
    }
}
