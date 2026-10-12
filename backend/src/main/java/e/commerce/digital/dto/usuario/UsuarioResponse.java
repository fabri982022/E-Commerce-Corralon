package e.commerce.digital.dto.usuario;

import e.commerce.digital.models.Usuario;
import e.commerce.digital.util.enumerated.Rol;

// Nunca incluye passwordHash
public record UsuarioResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        String email,
        String telefono,
        String direccion,
        String fotoUrl,
        Rol rol,
        Boolean activo
) {
    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(
                u.getIdUsuario(),
                u.getNombre(),
                u.getApellido(),
                u.getEmail(),
                u.getTelefono(),
                u.getDireccion(),
                u.getFotoUrl(),
                u.getRol(),
                u.getActivo());
    }
}
