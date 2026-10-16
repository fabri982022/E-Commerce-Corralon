package e.commerce.digital.service;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.producto.ProductoFiltroRequest;
import e.commerce.digital.dto.producto.ProductoRequest;
import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.ProductoMapper;
import e.commerce.digital.models.Producto;
import e.commerce.digital.models.Subcategoria;
import e.commerce.digital.repository.ProductoRepository;
import e.commerce.digital.repository.SubcategoriaRepository;
import e.commerce.digital.testutil.DatosDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService")
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private SubcategoriaRepository subcategoriaRepository;

    private ProductoService service;

    @BeforeEach
    void setUp() {
        service = new ProductoService(productoRepository, subcategoriaRepository, new ProductoMapper());
    }

    private Subcategoria latex() {
        return DatosDePrueba.subcategoria(1L, "Látex", DatosDePrueba.categoria(1L, "Pinturas", true), true);
    }

    private ProductoRequest requestValido(Boolean activo) {
        return new ProductoRequest(" Látex interior 20L ", "Lavable", new BigDecimal("15000.50"), 12, activo, 1L);
    }

    // ----- Administración -----

    @Test
    @DisplayName("crear: guarda el producto activo por defecto dentro de su subcategoría")
    void crear_ok() {
        when(subcategoriaRepository.findById(1L)).thenReturn(Optional.of(latex()));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoResponse r = service.crear(requestValido(null));

        assertThat(r.nombre()).isEqualTo("Látex interior 20L");
        assertThat(r.precio()).isEqualByComparingTo("15000.50");
        assertThat(r.stock()).isEqualTo(12);
        assertThat(r.activo()).isTrue();
        assertThat(r.idSubcategoria()).isEqualTo(1L);
        assertThat(r.nombreCategoria()).isEqualTo("Pinturas");
    }

    @Test
    @DisplayName("crear: la subcategoría debe existir")
    void crear_subcategoriaNoExiste() {
        when(subcategoriaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(requestValido(true)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Subcategoría");

        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar: modifica precio, existencias y subcategoría")
    void actualizar_ok() {
        Producto existente = DatosDePrueba.producto(5L, "Viejo", "100.00", 1);
        when(productoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(subcategoriaRepository.findById(1L)).thenReturn(Optional.of(latex()));

        ProductoResponse r = service.actualizar(5L, requestValido(false));

        assertThat(r.nombre()).isEqualTo("Látex interior 20L");
        assertThat(r.precio()).isEqualByComparingTo("15000.50");
        assertThat(r.stock()).isEqualTo(12);
        assertThat(r.activo()).isFalse();
    }

    @Test
    @DisplayName("actualizar: un producto inexistente responde no encontrado")
    void actualizar_noExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, requestValido(true)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("cambiarEstado: desactiva el producto")
    void cambiarEstado() {
        Producto existente = DatosDePrueba.producto(5L, "Látex", "100.00", 3);
        when(productoRepository.findById(5L)).thenReturn(Optional.of(existente));

        ProductoResponse r = service.cambiarEstado(5L, false);

        assertThat(r.activo()).isFalse();
    }

    @Test
    @DisplayName("obtenerAdmin: devuelve incluso un producto inactivo")
    void obtenerAdmin_inactivo() {
        Producto inactivo = DatosDePrueba.producto(5L, "Látex", "100.00", 3);
        inactivo.setActivo(false);
        when(productoRepository.findById(5L)).thenReturn(Optional.of(inactivo));

        assertThat(service.obtenerAdmin(5L).activo()).isFalse();
    }

    // ----- Catálogo público -----

    @Test
    @DisplayName("obtenerPublico: devuelve un producto disponible")
    void obtenerPublico_disponible() {
        when(productoRepository.findById(5L))
                .thenReturn(Optional.of(DatosDePrueba.producto(5L, "Látex", "100.00", 3)));

        assertThat(service.obtenerPublico(5L).nombre()).isEqualTo("Látex");
    }

    @Test
    @DisplayName("obtenerPublico: un producto inactivo responde como inexistente")
    void obtenerPublico_productoInactivo() {
        Producto inactivo = DatosDePrueba.producto(5L, "Látex", "100.00", 3);
        inactivo.setActivo(false);
        when(productoRepository.findById(5L)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> service.obtenerPublico(5L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("obtenerPublico: un producto de una categoría inactiva responde como inexistente")
    void obtenerPublico_categoriaInactiva() {
        Producto producto = DatosDePrueba.producto(5L, "Látex", "100.00", 3);
        producto.getSubcategoria().getCategoria().setActivo(false);
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.obtenerPublico(5L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("buscarPublico: un texto en blanco se ignora y solo se piden productos activos")
    void buscarPublico_textoEnBlanco() {
        Pageable pageable = PageRequest.of(0, 12);
        Producto producto = DatosDePrueba.producto(5L, "Látex", "100.00", 3);
        when(productoRepository.buscar(isNull(), eq(1L), isNull(), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(producto), pageable, 1));

        PageResponse<ProductoResponse> r =
                service.buscarPublico(new ProductoFiltroRequest("   ", 1L, null), pageable);

        assertThat(r.contenido()).hasSize(1);
        assertThat(r.totalElementos()).isEqualTo(1);
        assertThat(r.pagina()).isZero();
        assertThat(r.tamanio()).isEqualTo(12);
    }

    @Test
    @DisplayName("buscarPublico: el texto se recorta antes de consultar")
    void buscarPublico_textoSeRecorta() {
        Pageable pageable = PageRequest.of(0, 12);
        when(productoRepository.buscar(eq("latex"), isNull(), eq(2L), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<ProductoResponse> r =
                service.buscarPublico(new ProductoFiltroRequest(" latex ", null, 2L), pageable);

        assertThat(r.contenido()).isEmpty();
    }

    @Test
    @DisplayName("buscarPublico: un filtro nulo equivale a no filtrar")
    void buscarPublico_filtroNulo() {
        Pageable pageable = PageRequest.of(1, 5);
        when(productoRepository.buscar(isNull(), isNull(), isNull(), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(service.buscarPublico(null, pageable).contenido()).isEmpty();
    }

    @Test
    @DisplayName("buscarAdmin: incluye los productos inactivos")
    void buscarAdmin_incluyeInactivos() {
        Pageable pageable = PageRequest.of(0, 12);
        when(productoRepository.buscar(isNull(), isNull(), isNull(), eq(false), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.buscarAdmin(new ProductoFiltroRequest(null, null, null), pageable);

        verify(productoRepository).buscar(isNull(), isNull(), isNull(), eq(false), eq(pageable));
    }
}
