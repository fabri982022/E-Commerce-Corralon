package e.commerce.digital.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import e.commerce.digital.dto.common.ImagenResponse;
import e.commerce.digital.exception.ImagenInvalidaException;
import e.commerce.digital.exception.ServicioImagenesException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImagenService")
class ImagenServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 0x00};

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    private ImagenService service;

    @BeforeEach
    void setUp() {
        service = new ImagenService(cloudinary);
    }

    @AfterEach
    void limpiarSincronizacion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private MockMultipartFile archivo(String tipo, byte[] contenido) {
        return new MockMultipartFile("archivo", "foto.png", tipo, contenido);
    }

    // =====================  subir  =====================

    @Test
    @DisplayName("subir: devuelve la URL https y el public_id, con ruta de carpeta y UUID propios")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void subir_ok() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        doReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/x.png",
                "public_id", "ferreteria/usuarios/x"))
                .when(uploader).upload(any(), anyMap());

        ImagenResponse r = service.subir(archivo("image/png", PNG), ImagenService.CARPETA_USUARIOS);

        assertThat(r.url()).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/x.png");
        assertThat(r.publicId()).isEqualTo("ferreteria/usuarios/x");

        ArgumentCaptor<Map> opciones = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), opciones.capture());
        Map enviadas = opciones.getValue();
        assertThat((String) enviadas.get("public_id")).startsWith("ferreteria/usuarios/").doesNotContain("foto.png");
        assertThat(enviadas.get("asset_folder")).isEqualTo("ferreteria/usuarios");
        assertThat(enviadas).doesNotContainKey("folder"); // las carpetas dinámicas no usan "folder"
        assertThat(enviadas.get("resource_type")).isEqualTo("image");
        assertThat(enviadas.get("overwrite")).isEqualTo(false);
    }

    @Test
    @DisplayName("subir: dos subidas generan public_id distintos")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void subir_publicIdUnico() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        doReturn(Map.of("secure_url", "https://x", "public_id", "p")).when(uploader).upload(any(), anyMap());

        service.subir(archivo("image/png", PNG), ImagenService.CARPETA_PRODUCTOS);
        service.subir(archivo("image/png", PNG), ImagenService.CARPETA_PRODUCTOS);

        ArgumentCaptor<Map> opciones = ArgumentCaptor.forClass(Map.class);
        verify(uploader, org.mockito.Mockito.times(2)).upload(any(), opciones.capture());
        assertThat(opciones.getAllValues().get(0).get("public_id"))
                .isNotEqualTo(opciones.getAllValues().get(1).get("public_id"));
    }

    @Test
    @DisplayName("subir: acepta JPEG y WebP además de PNG")
    void subir_otrosFormatosValidos() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        doReturn(Map.of("secure_url", "https://x", "public_id", "p")).when(uploader).upload(any(), anyMap());

        assertThat(service.subir(archivo("image/jpeg", JPEG), "ferreteria/productos").url()).isEqualTo("https://x");
        assertThat(service.subir(archivo("image/webp", WEBP), "ferreteria/productos").url()).isEqualTo("https://x");
    }

    @Test
    @DisplayName("subir: rechaza un archivo nulo o vacío")
    void subir_vacio() {
        assertThatThrownBy(() -> service.subir(null, "ferreteria/usuarios"))
                .isInstanceOf(ImagenInvalidaException.class)
                .hasMessageContaining("seleccionar");
        assertThatThrownBy(() -> service.subir(archivo("image/png", new byte[0]), "ferreteria/usuarios"))
                .isInstanceOf(ImagenInvalidaException.class)
                .hasMessageContaining("seleccionar");

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("subir: rechaza una imagen de más de 5 MB")
    void subir_demasiadoGrande() {
        byte[] grande = new byte[5 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> service.subir(archivo("image/png", grande), "ferreteria/usuarios"))
                .isInstanceOf(ImagenInvalidaException.class)
                .hasMessageContaining("5 MB");

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("subir: rechaza un tipo de archivo no permitido")
    void subir_tipoNoPermitido() {
        assertThatThrownBy(() -> service.subir(archivo("image/gif", PNG), "ferreteria/usuarios"))
                .isInstanceOf(ImagenInvalidaException.class)
                .hasMessageContaining("Formato");
        assertThatThrownBy(() -> service.subir(archivo("application/pdf", PNG), "ferreteria/usuarios"))
                .isInstanceOf(ImagenInvalidaException.class);

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("subir: rechaza un archivo cuyo contenido no coincide con el tipo declarado")
    void subir_firmaFalsa() {
        byte[] texto = "esto no es una imagen".getBytes();

        assertThatThrownBy(() -> service.subir(archivo("image/png", texto), "ferreteria/usuarios"))
                .isInstanceOf(ImagenInvalidaException.class)
                .hasMessageContaining("no es una imagen válida");

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("subir: si Cloudinary falla, responde con un error del servicio de imágenes")
    void subir_fallaCloudinary() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        doThrow(new IOException("sin conexión")).when(uploader).upload(any(), anyMap());

        assertThatThrownBy(() -> service.subir(archivo("image/png", PNG), "ferreteria/usuarios"))
                .isInstanceOf(ServicioImagenesException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    // =====================  eliminar  =====================

    @Test
    @DisplayName("eliminar: con public_id nulo o vacío no llama a Cloudinary")
    void eliminar_sinPublicId() {
        service.eliminar(null);
        service.eliminar("");
        service.eliminar("   ");

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("eliminar: destruye la imagen en Cloudinary")
    void eliminar_ok() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        doReturn(Map.of("result", "ok")).when(uploader).destroy(anyString(), anyMap());

        service.eliminar("ferreteria/usuarios/x");

        verify(uploader).destroy(eq("ferreteria/usuarios/x"), anyMap());
    }

    @Test
    @DisplayName("eliminar: si Cloudinary falla, responde con un error del servicio de imágenes")
    void eliminar_fallaCloudinary() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        doThrow(new IOException("sin conexión")).when(uploader).destroy(anyString(), anyMap());

        assertThatThrownBy(() -> service.eliminar("ferreteria/usuarios/x"))
                .isInstanceOf(ServicioImagenesException.class);
    }

    // =====================  limpieza coordinada con la transacción  =====================

    private void finalizarTransaccion(int estado) {
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(estado));
    }

    @Test
    @DisplayName("limpiar: si la transacción confirma, se borra la imagen anterior")
    void limpiar_commitBorraLaAnterior() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        when(cloudinary.uploader()).thenReturn(uploader);
        doReturn(Map.of("result", "ok")).when(uploader).destroy(anyString(), anyMap());

        service.limpiarAlFinalizarTransaccion("ferreteria/usuarios/vieja", "ferreteria/usuarios/nueva");
        finalizarTransaccion(TransactionSynchronization.STATUS_COMMITTED);

        verify(uploader).destroy(eq("ferreteria/usuarios/vieja"), anyMap());
        verify(uploader, never()).destroy(eq("ferreteria/usuarios/nueva"), anyMap());
    }

    @Test
    @DisplayName("limpiar: si la transacción se revierte, se borra la imagen nueva")
    void limpiar_rollbackBorraLaNueva() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        when(cloudinary.uploader()).thenReturn(uploader);
        doReturn(Map.of("result", "ok")).when(uploader).destroy(anyString(), anyMap());

        service.limpiarAlFinalizarTransaccion("ferreteria/usuarios/vieja", "ferreteria/usuarios/nueva");
        finalizarTransaccion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(uploader).destroy(eq("ferreteria/usuarios/nueva"), anyMap());
        verify(uploader, never()).destroy(eq("ferreteria/usuarios/vieja"), anyMap());
    }

    @Test
    @DisplayName("limpiar: si no había imagen anterior, al confirmar no se llama a Cloudinary")
    void limpiar_sinImagenAnterior() {
        TransactionSynchronizationManager.initSynchronization();

        service.limpiarAlFinalizarTransaccion(null, "ferreteria/usuarios/nueva");
        finalizarTransaccion(TransactionSynchronization.STATUS_COMMITTED);

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("limpiar: un fallo al borrar no interrumpe la operación del usuario")
    void limpiar_falloAlBorrarEsSilencioso() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        when(cloudinary.uploader()).thenReturn(uploader);
        doThrow(new IOException("sin conexión")).when(uploader).destroy(anyString(), anyMap());

        service.limpiarAlFinalizarTransaccion("ferreteria/usuarios/vieja", "ferreteria/usuarios/nueva");

        assertThatCode(() -> finalizarTransaccion(TransactionSynchronization.STATUS_COMMITTED))
                .doesNotThrowAnyException();
    }
}
