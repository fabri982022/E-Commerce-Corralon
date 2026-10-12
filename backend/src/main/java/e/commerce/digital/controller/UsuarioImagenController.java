package e.commerce.digital.controller;

import e.commerce.digital.dto.usuario.UsuarioResponse;
import e.commerce.digital.service.UsuarioImagenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// Foto del usuario autenticado: no recibe ningún id, siempre opera sobre la propia cuenta
@RestController
@RequestMapping("/api/usuarios/me/foto")
@RequiredArgsConstructor
public class UsuarioImagenController {

    private final UsuarioImagenService usuarioImagenService;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UsuarioResponse subir(@RequestPart("archivo") MultipartFile archivo, Authentication auth) {
        return usuarioImagenService.actualizarFoto(auth.getName(), archivo);
    }

    @DeleteMapping
    public UsuarioResponse eliminar(Authentication auth) {
        return usuarioImagenService.eliminarFoto(auth.getName());
    }
}
