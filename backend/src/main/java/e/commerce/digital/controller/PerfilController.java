package e.commerce.digital.controller;

import e.commerce.digital.dto.usuario.UsuarioResponse;
import e.commerce.digital.dto.usuario.UsuarioUpdateRequest;
import e.commerce.digital.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfil del usuario autenticado (cliente o administrador). La foto está en UsuarioImagenController. */
@RestController
@RequestMapping("/api/usuarios/me")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioService usuarioService;

    @GetMapping
    public UsuarioResponse obtener(Authentication auth) {
        return usuarioService.obtenerPerfil(auth.getName());
    }

    @PutMapping
    public UsuarioResponse actualizar(@Valid @RequestBody UsuarioUpdateRequest req, Authentication auth) {
        return usuarioService.actualizarPerfil(auth.getName(), req);
    }
}
