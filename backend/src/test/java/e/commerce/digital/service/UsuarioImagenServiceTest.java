package e.commerce.digital.service;

import e.commerce.digital.dto.common.ImagenResponse;
import e.commerce.digital.dto.usuario.UsuarioResponse;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.exception.ServicioImagenesException;
import e.commerce.digital.mapper.UsuarioMapper;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.UsuarioRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioImagenService")
class UsuarioImagenServiceTest {

    private static final String EMAIL = "ana@mail.com";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ImagenService imagenService;

    private UsuarioImagenService service;
    private UsuarioCliente ana;
    private final MockMultipartFile archivo =
            new MockMultipartFile("archivo", "foto.png", "image/png", new byte[]{1, 2, 3});

    @BeforeEach
    void setUp() {
        service = new UsuarioImagenService(usuarioRepository, new UsuarioMapper(), imagenService);
        ana = DatosDePrueba.cliente(1L, EMAIL);
    }

    @Test
    @DisplayName("actualizarFoto: guarda URL y public_id nuevos y programa el borrado de la foto anterior")
    void actualizarFoto_reemplaza() {
        ana.setFotoUrl("https://res.cloudinary.com/demo/vieja.png");
        ana.setFotoPublicId("ferreteria/usuarios/vieja");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(imagenService.subir(archivo, ImagenService.CARPETA_USUARIOS))
                .thenReturn(new ImagenResponse("https://res.cloudinary.com/demo/nueva.png", "ferreteria/usuarios/nueva"));
        when(usuarioRepository.save(ana)).thenReturn(ana);

        UsuarioResponse r = service.actualizarFoto(EMAIL, archivo);

        assertThat(r.fotoUrl()).isEqualTo("https://res.cloudinary.com/demo/nueva.png");
        assertThat(ana.getFotoPublicId()).isEqualTo("ferreteria/usuarios/nueva");
        verify(imagenService).limpiarAlFinalizarTransaccion("ferreteria/usuarios/vieja", "ferreteria/usuarios/nueva");
    }

    @Test
    @DisplayName("actualizarFoto: la primera foto no tiene anterior que borrar")
    void actualizarFoto_primeraVez() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(imagenService.subir(archivo, ImagenService.CARPETA_USUARIOS))
                .thenReturn(new ImagenResponse("https://x/nueva.png", "ferreteria/usuarios/nueva"));
        when(usuarioRepository.save(ana)).thenReturn(ana);

        service.actualizarFoto(EMAIL, archivo);

        verify(imagenService).limpiarAlFinalizarTransaccion(null, "ferreteria/usuarios/nueva");
    }

    @Test
    @DisplayName("actualizarFoto: un usuario inexistente responde no encontrado sin subir nada")
    void actualizarFoto_usuarioNoExiste() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarFoto(EMAIL, archivo))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(imagenService);
    }

    @Test
    @DisplayName("actualizarFoto: si la subida falla, no se modifica el usuario ni se guarda")
    void actualizarFoto_fallaLaSubida() {
        ana.setFotoUrl("https://res.cloudinary.com/demo/vieja.png");
        ana.setFotoPublicId("ferreteria/usuarios/vieja");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(imagenService.subir(any(), eq(ImagenService.CARPETA_USUARIOS)))
                .thenThrow(new ServicioImagenesException("falla", new java.io.IOException()));

        assertThatThrownBy(() -> service.actualizarFoto(EMAIL, archivo))
                .isInstanceOf(ServicioImagenesException.class);

        assertThat(ana.getFotoPublicId()).isEqualTo("ferreteria/usuarios/vieja");
        verify(usuarioRepository, never()).save(any());
        verify(imagenService, never()).limpiarAlFinalizarTransaccion(any(), any());
    }

    @Test
    @DisplayName("eliminarFoto: borra URL y public_id y programa el borrado en Cloudinary")
    void eliminarFoto_ok() {
        ana.setFotoUrl("https://res.cloudinary.com/demo/vieja.png");
        ana.setFotoPublicId("ferreteria/usuarios/vieja");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(usuarioRepository.save(ana)).thenReturn(ana);

        UsuarioResponse r = service.eliminarFoto(EMAIL);

        assertThat(r.fotoUrl()).isNull();
        assertThat(ana.getFotoPublicId()).isNull();
        verify(imagenService).limpiarAlFinalizarTransaccion("ferreteria/usuarios/vieja", null);
    }

    @Test
    @DisplayName("eliminarFoto: un usuario inexistente responde no encontrado")
    void eliminarFoto_usuarioNoExiste() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminarFoto(EMAIL)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
