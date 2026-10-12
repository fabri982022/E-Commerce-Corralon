package e.commerce.digital.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// El cliente edita su propio perfil; email, rol y activo no se modifican desde acá
public record UsuarioUpdateRequest(
        @NotBlank @Size(max = 50) String nombre,
        @NotBlank @Size(max = 50) String apellido,
        @Size(max = 15) String telefono,
        @Size(max = 255) String direccion
) {
}
