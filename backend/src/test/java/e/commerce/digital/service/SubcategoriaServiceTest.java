package e.commerce.digital.service;

import e.commerce.digital.dto.subcategoria.SubcategoriaRequest;
import e.commerce.digital.dto.subcategoria.SubcategoriaResponse;
import e.commerce.digital.exception.ConflictoException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.SubcategoriaMapper;
import e.commerce.digital.models.Categoria;
import e.commerce.digital.models.Subcategoria;
import e.commerce.digital.repository.CategoriaRepository;
import e.commerce.digital.repository.SubcategoriaRepository;
import e.commerce.digital.testutil.DatosDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubcategoriaService")
class SubcategoriaServiceTest {

    @Mock
    private SubcategoriaRepository subcategoriaRepository;
    @Mock
    private CategoriaRepository categoriaRepository;

    private SubcategoriaService service;

    @BeforeEach
    void setUp() {
        service = new SubcategoriaService(subcategoriaRepository, categoriaRepository, new SubcategoriaMapper());
    }

    @Test
    @DisplayName("crear: guarda la subcategoría dentro de su categoría")
    void crear_ok() {
        Categoria pinturas = DatosDePrueba.categoria(1L, "Pinturas", true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(pinturas));
        when(subcategoriaRepository.existsByNombreAndCategoria_IdCategoria("Látex", 1L)).thenReturn(false);
        when(subcategoriaRepository.save(any(Subcategoria.class))).thenAnswer(inv -> inv.getArgument(0));

        SubcategoriaResponse r = service.crear(new SubcategoriaRequest(" Látex ", null, null, 1L));

        assertThat(r.nombre()).isEqualTo("Látex");
        assertThat(r.activo()).isTrue();
        assertThat(r.idCategoria()).isEqualTo(1L);
        assertThat(r.nombreCategoria()).isEqualTo("Pinturas");
    }

    @Test
    @DisplayName("crear: la categoría debe existir")
    void crear_categoriaNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new SubcategoriaRequest("Látex", null, null, 99L)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Categoría");

        verify(subcategoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear: rechaza un nombre repetido dentro de la misma categoría")
    void crear_nombreDuplicadoEnCategoria() {
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(DatosDePrueba.categoria(1L, "Pinturas", true)));
        when(subcategoriaRepository.existsByNombreAndCategoria_IdCategoria("Látex", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new SubcategoriaRequest("Látex", null, null, 1L)))
                .isInstanceOf(ConflictoException.class);

        verify(subcategoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar: permite mover la subcategoría a otra categoría")
    void actualizar_cambiaDeCategoria() {
        Categoria pinturas = DatosDePrueba.categoria(1L, "Pinturas", true);
        Categoria ventanas = DatosDePrueba.categoria(2L, "Ventanas", true);
        Subcategoria existente = DatosDePrueba.subcategoria(5L, "PVC", pinturas, true);
        when(subcategoriaRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(ventanas));
        when(subcategoriaRepository.existsByNombreAndCategoria_IdCategoriaAndIdSubcategoriaNot("PVC", 2L, 5L))
                .thenReturn(false);

        SubcategoriaResponse r = service.actualizar(5L, new SubcategoriaRequest("PVC", "Marcos de PVC", true, 2L));

        assertThat(r.idCategoria()).isEqualTo(2L);
        assertThat(r.nombreCategoria()).isEqualTo("Ventanas");
        assertThat(r.descripcion()).isEqualTo("Marcos de PVC");
    }

    @Test
    @DisplayName("actualizar: rechaza el nombre de otra subcategoría de la categoría")
    void actualizar_nombreDuplicado() {
        Categoria pinturas = DatosDePrueba.categoria(1L, "Pinturas", true);
        when(subcategoriaRepository.findById(5L))
                .thenReturn(Optional.of(DatosDePrueba.subcategoria(5L, "Látex", pinturas, true)));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(pinturas));
        when(subcategoriaRepository.existsByNombreAndCategoria_IdCategoriaAndIdSubcategoriaNot("Esmaltes", 1L, 5L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(5L, new SubcategoriaRequest("Esmaltes", null, null, 1L)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("actualizar: una subcategoría inexistente responde no encontrada")
    void actualizar_noExiste() {
        when(subcategoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, new SubcategoriaRequest("X", null, null, 1L)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(categoriaRepository);
    }

    @Test
    @DisplayName("cambiarEstado: desactiva la subcategoría")
    void cambiarEstado() {
        Subcategoria existente = DatosDePrueba.subcategoria(5L, "Látex",
                DatosDePrueba.categoria(1L, "Pinturas", true), true);
        when(subcategoriaRepository.findById(5L)).thenReturn(Optional.of(existente));

        SubcategoriaResponse r = service.cambiarEstado(5L, false);

        assertThat(r.activo()).isFalse();
    }

    @Test
    @DisplayName("obtener: una subcategoría inexistente responde no encontrada")
    void obtener_noExiste() {
        when(subcategoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("listarPorCategoria: con soloActivas usa la consulta de activas")
    void listarPorCategoria_soloActivas() {
        Categoria pinturas = DatosDePrueba.categoria(1L, "Pinturas", true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(pinturas));
        when(subcategoriaRepository.findByCategoria_IdCategoriaAndActivoTrueOrderByNombreAsc(1L))
                .thenReturn(List.of(DatosDePrueba.subcategoria(5L, "Látex", pinturas, true)));

        List<SubcategoriaResponse> lista = service.listarPorCategoria(1L, true);

        assertThat(lista).extracting(SubcategoriaResponse::nombre).containsExactly("Látex");
        verify(subcategoriaRepository, never()).findByCategoria_IdCategoriaOrderByNombreAsc(any());
    }

    @Test
    @DisplayName("listarPorCategoria: sin soloActivas incluye las inactivas")
    void listarPorCategoria_todas() {
        Categoria pinturas = DatosDePrueba.categoria(1L, "Pinturas", true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(pinturas));
        when(subcategoriaRepository.findByCategoria_IdCategoriaOrderByNombreAsc(1L)).thenReturn(List.of(
                DatosDePrueba.subcategoria(5L, "Látex", pinturas, true),
                DatosDePrueba.subcategoria(6L, "Aerosoles", pinturas, false)));

        List<SubcategoriaResponse> lista = service.listarPorCategoria(1L, false);

        assertThat(lista).hasSize(2);
    }

    @Test
    @DisplayName("listarPorCategoria: una categoría inexistente responde no encontrada")
    void listarPorCategoria_categoriaNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPorCategoria(99L, true))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(subcategoriaRepository);
    }
}
