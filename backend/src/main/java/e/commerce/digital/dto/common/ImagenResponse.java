package e.commerce.digital.dto.common;

// Resultado de subir una imagen a Cloudinary (perfil de usuario o producto)
public record ImagenResponse(
        String url,
        String publicId
) {
}
