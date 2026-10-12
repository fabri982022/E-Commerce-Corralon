package e.commerce.digital.dto.usuario;

import e.commerce.digital.util.enumerated.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Uso exclusivo del administrador: crea un usuario con el rol indicado
// (el service instancia UsuarioCliente o UsuarioAdmin según el rol)
public record UsuarioAdminRequest(
        @NotBlank @Size(max = 50) String nombre,
        @NotBlank @Size(max = 50) String apellido,
        @NotBlank @Email @Size(max = 50) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 15) String telefono,
        @Size(max = 255) String direccion,
        @NotNull Rol rol
) {
}
