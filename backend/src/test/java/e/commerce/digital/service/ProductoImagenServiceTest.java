package e.commerce.digital.service;

import e.commerce.digital.dto.common.ImagenResponse;
import e.commerce.digital.dto.producto.ProductoResponse;
import e.commerce.digital.exception.ImagenInvalidaException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.ProductoMapper;
import e.commerce.digital.models.Producto;
import e.commerce.digital.repository.ProductoRepository;
import e.commerce.digital.testutil.DatosDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoImagenService")
class ProductoImagenServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ImagenService imagenService;

    private ProductoImagenService service;
    private Producto latex;
    private final MockMultipartFile archivo =
            new MockMultipartFile("archivo", "latex.png", "image/png", new byte[]{1, 2, 3});

    @BeforeEach
    void setUp() {
        service = new ProductoImagenService(productoRepository, new ProductoMapper(), imagenService);
        latex = DatosDePrueba.producto(5L, "Látex", "100.00", 3);
    }

    @Test
    @DisplayName("actualizarImagen: guarda URL y public_id nuevos y programa el borrado de la imagen anterior")
    void actualizarImagen_reemplaza() {
        latex.setImagenUrl("https://res.cloudinary.com/demo/vieja.png");
        latex.setImagenPublicId("ferreteria/productos/vieja");
        when(productoRepository.findById(5L)).thenReturn(Optional.of(latex));
        when(imagenService.subir(archivo, ImagenService.CARPETA_PRODUCTOS))
                .thenReturn(new ImagenResponse("https://res.cloudinary.com/demo/nueva.png", "ferreteria/productos/nueva"));
        when(productoRepository.save(latex)).thenReturn(latex);

        ProductoResponse r = service.actualizarImagen(5L, archivo);

        assertThat(r.imagenUrl()).isEqualTo("https://res.cloudinary.com/demo/nueva.png");
        assertThat(latex.getImagenPublicId()).isEqualTo("ferreteria/productos/nueva");
        verify(imagenService).limpiarAlFinalizarTransaccion("ferreteria/productos/vieja", "ferreteria/productos/nueva");
    }

    @Test
    @DisplayName("actualizarImagen: un producto inexistente responde no encontrado sin subir nada")
    void actualizarImagen_productoNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarImagen(99L, archivo))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(imagenService);
    }

    @Test
    @DisplayName("actualizarImagen: si la imagen no es válida, no se modifica el producto")
    void actualizarImagen_imagenInvalida() {
        latex.setImagenPublicId("ferreteria/productos/vieja");
        when(productoRepository.findById(5L)).thenReturn(Optional.of(latex));
        when(imagenService.subir(archivo, ImagenService.CARPETA_PRODUCTOS))
                .thenThrow(new ImagenInvalidaException("Formato no permitido"));

        assertThatThrownBy(() -> service.actualizarImagen(5L, archivo))
                .isInstanceOf(ImagenInvalidaException.class);

        assertThat(latex.getImagenPublicId()).isEqualTo("ferreteria/productos/vieja");
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminarImagen: borra URL y public_id y programa el borrado en Cloudinary")
    void eliminarImagen_ok() {
        latex.setImagenUrl("https://res.cloudinary.com/demo/vieja.png");
        latex.setImagenPublicId("ferreteria/productos/vieja");
        when(productoRepository.findById(5L)).thenReturn(Optional.of(latex));
        when(productoRepository.save(latex)).thenReturn(latex);

        ProductoResponse r = service.eliminarImagen(5L);

        assertThat(r.imagenUrl()).isNull();
        assertThat(latex.getImagenPublicId()).isNull();
        verify(imagenService).limpiarAlFinalizarTransaccion("ferreteria/productos/vieja", null);
    }

    @Test
    @DisplayName("eliminarImagen: un producto inexistente responde no encontrado")
    void eliminarImagen_productoNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminarImagen(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
