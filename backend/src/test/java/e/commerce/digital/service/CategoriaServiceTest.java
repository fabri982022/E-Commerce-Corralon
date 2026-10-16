package e.commerce.digital.service;

import e.commerce.digital.dto.categoria.CategoriaRequest;
import e.commerce.digital.dto.categoria.CategoriaResponse;
import e.commerce.digital.exception.ConflictoException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.CategoriaMapper;
import e.commerce.digital.models.Categoria;
import e.commerce.digital.repository.CategoriaRepository;
import e.commerce.digital.testutil.DatosDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoriaService")
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        service = new CategoriaService(categoriaRepository, new CategoriaMapper());
    }

    @Test
    @DisplayName("crear: guarda la categoría con el nombre sin espacios y activa por defecto")
    void crear_ok() {
        when(categoriaRepository.existsByNombre("Pinturas")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaResponse r = service.crear(new CategoriaRequest("  Pinturas ", "Pinturas para interior", null));

        assertThat(r.nombre()).isEqualTo("Pinturas");
        assertThat(r.descripcion()).isEqualTo("Pinturas para interior");
        assertThat(r.activo()).isTrue();
    }

    @Test
    @DisplayName("crear: rechaza un nombre que ya existe")
    void crear_nombreDuplicado() {
        when(categoriaRepository.existsByNombre("Pinturas")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CategoriaRequest("Pinturas", null, null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Ya existe");

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar: modifica nombre, descripción y estado")
    void actualizar_ok() {
        Categoria existente = DatosDePrueba.categoria(1L, "Viejo", true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existsByNombreAndIdCategoriaNot("Nuevo", 1L)).thenReturn(false);

        CategoriaResponse r = service.actualizar(1L, new CategoriaRequest(" Nuevo ", "Otra descripción", false));

        assertThat(r.nombre()).isEqualTo("Nuevo");
        assertThat(r.descripcion()).isEqualTo("Otra descripción");
        assertThat(r.activo()).isFalse();
    }

    @Test
    @DisplayName("actualizar: si el activo viene nulo, conserva el estado actual")
    void actualizar_activoNuloConservaEstado() {
        Categoria existente = DatosDePrueba.categoria(1L, "Pinturas", true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existsByNombreAndIdCategoriaNot("Pinturas", 1L)).thenReturn(false);

        CategoriaResponse r = service.actualizar(1L, new CategoriaRequest("Pinturas", null, null));

        assertThat(r.activo()).isTrue();
    }

    @Test
    @DisplayName("actualizar: rechaza el nombre de otra categoría")
    void actualizar_nombreDuplicado() {
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(DatosDePrueba.categoria(1L, "Pinturas", true)));
        when(categoriaRepository.existsByNombreAndIdCategoriaNot("Ventanas", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, new CategoriaRequest("Ventanas", null, null)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("actualizar: una categoría inexistente responde no encontrada")
    void actualizar_noExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, new CategoriaRequest("X", null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("cambiarEstado: desactiva la categoría")
    void cambiarEstado_desactiva() {
        Categoria existente = DatosDePrueba.categoria(1L, "Pinturas", true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));

        CategoriaResponse r = service.cambiarEstado(1L, false);

        assertThat(r.activo()).isFalse();
        assertThat(existente.getActivo()).isFalse();
    }

    @Test
    @DisplayName("obtener: devuelve la categoría")
    void obtener_ok() {
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(DatosDePrueba.categoria(1L, "Pinturas", true)));

        assertThat(service.obtener(1L).nombre()).isEqualTo("Pinturas");
    }

    @Test
    @DisplayName("obtener: una categoría inexistente responde no encontrada")
    void obtener_noExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("listarActivas: usa la consulta de categorías activas")
    void listarActivas() {
        when(categoriaRepository.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(
                DatosDePrueba.categoria(1L, "Pinturas", true),
                DatosDePrueba.categoria(2L, "Puertas", true)));

        List<CategoriaResponse> lista = service.listarActivas();

        assertThat(lista).extracting(CategoriaResponse::nombre).containsExactly("Pinturas", "Puertas");
    }

    @Test
    @DisplayName("listarTodas: incluye activas e inactivas, ordenadas por nombre")
    void listarTodas() {
        when(categoriaRepository.findAll(Sort.by("nombre"))).thenReturn(List.of(
                DatosDePrueba.categoria(1L, "Pinturas", true),
                DatosDePrueba.categoria(3L, "Sanitarios", false)));

        List<CategoriaResponse> lista = service.listarTodas();

        assertThat(lista).hasSize(2);
        assertThat(lista).extracting(CategoriaResponse::activo).containsExactly(true, false);
    }
}
