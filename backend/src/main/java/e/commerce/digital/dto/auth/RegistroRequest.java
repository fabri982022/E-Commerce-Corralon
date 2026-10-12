package e.commerce.digital.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Size(max = 50) String nombre,
        @NotBlank @Size(max = 50) String apellido,
        @NotBlank @Email @Size(max = 50) String email,
        // BCrypt procesa como máximo 72 bytes
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 15) String telefono,
        @Size(max = 255) String direccion
) {
}
