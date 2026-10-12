package e.commerce.digital.dto.usuario;

import jakarta.validation.constraints.NotNull;

// Uso exclusivo del administrador para activar o desactivar un usuario
public record UsuarioEstadoRequest(
        @NotNull Boolean activo
) {
}
