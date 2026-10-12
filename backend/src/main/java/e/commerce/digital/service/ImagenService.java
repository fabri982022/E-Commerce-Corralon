package e.commerce.digital.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import e.commerce.digital.dto.common.ImagenResponse;
import e.commerce.digital.exception.ImagenInvalidaException;
import e.commerce.digital.exception.ServicioImagenesException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Único punto de contacto con Cloudinary. Ningún otro service ni controller usa el SDK directamente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImagenService {

    public static final String CARPETA_USUARIOS = "ferreteria/usuarios";
    public static final String CARPETA_PRODUCTOS = "ferreteria/productos";

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final Cloudinary cloudinary;

    /** Valida y sube la imagen a la carpeta indicada. Devuelve la URL https y el public_id. */
    public ImagenResponse subir(MultipartFile archivo, String carpeta) {
        validar(archivo);
        try {
            byte[] bytes = archivo.getBytes();
            validarFirma(bytes);

            // Las cuentas nuevas de Cloudinary usan carpetas dinámicas: el parámetro "folder" no se debe usar.
            // Se fija el public_id con la ruta y un UUID (nunca el nombre que envía el cliente)
            // y asset_folder con la misma ruta, para que la imagen se vea en esa carpeta del panel.
            String publicId = carpeta + "/" + UUID.randomUUID();

            Map<?, ?> resultado = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "asset_folder", carpeta,
                    "resource_type", "image",
                    "overwrite", false));

            return new ImagenResponse(
                    (String) resultado.get("secure_url"),
                    (String) resultado.get("public_id"));
        } catch (IOException e) {
            throw new ServicioImagenesException("No se pudo subir la imagen", e);
        }
    }

    /** Borra la imagen. Si publicId es nulo o vacío no hace nada. */
    public void eliminar(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new ServicioImagenesException("No se pudo eliminar la imagen", e);
        }
    }

    /**
     * Limpieza coordinada con la transacción de base de datos. Debe llamarse dentro de un método @Transactional.
     * - Si la transacción confirma, se borra la imagen anterior (ya no se usa).
     * - Si hace rollback, se borra la imagen nueva (quedó huérfana).
     * Así nunca se pierde la imagen vigente por un fallo a mitad de camino.
     */
    public void limpiarAlFinalizarTransaccion(String publicIdAnterior, String publicIdNueva) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                eliminarSilencioso(status == STATUS_COMMITTED ? publicIdAnterior : publicIdNueva);
            }
        });
    }

    // Un fallo al borrar no debe romper la operación del usuario: se registra y se sigue
    private void eliminarSilencioso(String publicId) {
        try {
            eliminar(publicId);
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar la imagen {} de Cloudinary", publicId, e);
        }
    }

    private void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ImagenInvalidaException("Debe seleccionar una imagen");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ImagenInvalidaException("La imagen no puede superar los 5 MB");
        }
        if (archivo.getContentType() == null || !TIPOS_PERMITIDOS.contains(archivo.getContentType())) {
            throw new ImagenInvalidaException("Formato no permitido. Use JPG, PNG o WebP");
        }
    }

    // El Content-Type lo envía el cliente y se puede falsificar: se verifica el contenido real
    private void validarFirma(byte[] b) {
        boolean jpeg = b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        boolean png = b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean webp = b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        if (!(jpeg || png || webp)) {
            throw new ImagenInvalidaException("El archivo no es una imagen válida");
        }
    }
}
