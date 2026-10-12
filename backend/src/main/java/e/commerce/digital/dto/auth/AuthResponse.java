package e.commerce.digital.dto.auth;

import e.commerce.digital.dto.usuario.UsuarioResponse;

public record AuthResponse(
        String token,
        String tipo,
        UsuarioResponse usuario
) {
    public static AuthResponse bearer(String token, UsuarioResponse usuario) {
        return new AuthResponse(token, "Bearer", usuario);
    }
}
