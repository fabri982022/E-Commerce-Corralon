package e.commerce.digital.service;

import e.commerce.digital.dto.producto.ProductoVendedorRequest;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.ProductoMapper;
import e.commerce.digital.models.Categoria;
import e.commerce.digital.models.Producto;
import e.commerce.digital.models.Subcategoria;
import e.commerce.digital.models.UsuarioVendedor;
import e.commerce.digital.repository.ProductoRepository;
import e.commerce.digital.repository.SubcategoriaRepository;
import e.commerce.digital.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendedorProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private SubcategoriaRepository subcategoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private VendedorProductoService service;

    @BeforeEach
    void setUp() {
        service = new VendedorProductoService(
                productoRepository, subcategoriaRepository, usuarioRepository, new ProductoMapper());
    }

    @Test
    void crearProductoLoDejaPendienteYLoAsignaAlVendedor() {
        UsuarioVendedor vendedor = vendedor();
        when(usuarioRepository.findByEmail(vendedor.getEmail())).thenReturn(Optional.of(vendedor));
        when(subcategoriaRepository.findById(3L)).thenReturn(Optional.of(subcategoria()));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.crear(vendedor.getEmail(), request());

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertThat(captor.getValue().getVendedor()).isSameAs(vendedor);
        assertThat(captor.getValue().getActivo()).isFalse();
        assertThat(response.activo()).isFalse();
        assertThat(response.publicadoPorVendedor()).isTrue();
        assertThat(response.idSubcategoria()).isEqualTo(3L);
    }

    @Test
    void actualizarNoPermiteModificarProductoDeOtroVendedor() {
        UsuarioVendedor vendedor = vendedor();
        when(usuarioRepository.findByEmail(vendedor.getEmail())).thenReturn(Optional.of(vendedor));
        when(productoRepository.findByIdProductoAndVendedor_Email(12L, vendedor.getEmail()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(vendedor.getEmail(), 12L, request()))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(subcategoriaRepository, never()).findById(any());
        verify(productoRepository, never()).save(any());
    }

    private UsuarioVendedor vendedor() {
        UsuarioVendedor vendedor = new UsuarioVendedor();
        vendedor.setIdUsuario(7L);
        vendedor.setEmail("seller@example.com");
        vendedor.setActivo(true);
        return vendedor;
    }

    private Subcategoria subcategoria() {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(2L);
        categoria.setNombre("Pinturas");
        categoria.setActivo(true);

        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setIdSubcategoria(3L);
        subcategoria.setNombre("Látex");
        subcategoria.setCategoria(categoria);
        subcategoria.setActivo(true);
        return subcategoria;
    }

    private ProductoVendedorRequest request() {
        return new ProductoVendedorRequest("Pintura", "Interior", new BigDecimal("1999.90"), 5, 3L);
    }
}
