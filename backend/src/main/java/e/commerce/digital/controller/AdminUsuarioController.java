package e.commerce.digital.controller;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.usuario.UsuarioAdminRequest;
import e.commerce.digital.dto.usuario.UsuarioEstadoRequest;
import e.commerce.digital.dto.usuario.UsuarioResponse;
import e.commerce.digital.service.UsuarioService;
import e.commerce.digital.util.enumerated.Rol;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de usuarios (RF01): alta con rol, consulta y activación. */
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    /** Filtro opcional por rol (CLIENTE o ADMIN). */
    @GetMapping
    public PageResponse<UsuarioResponse> listar(
            @RequestParam(required = false) Rol rol,
            @PageableDefault(size = 20, sort = "apellido") Pageable pageable) {
        return usuarioService.listar(rol, pageable);
    }

    @GetMapping("/{idUsuario}")
    public UsuarioResponse obtener(@PathVariable Long idUsuario) {
        return usuarioService.obtenerPorId(idUsuario);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioAdminRequest req) {
        UsuarioResponse creado = usuarioService.crearPorAdmin(req);
        return Respuestas.creado(creado, "/api/admin/usuarios/{id}", creado.idUsuario());
    }

    /** Un administrador no puede desactivar su propia cuenta (el service lo valida con el email de la sesión). */
    @PatchMapping("/{idUsuario}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long idUsuario,
                                         @Valid @RequestBody UsuarioEstadoRequest req,
                                         Authentication auth) {
        return usuarioService.cambiarEstado(idUsuario, req, auth.getName());
    }
}
