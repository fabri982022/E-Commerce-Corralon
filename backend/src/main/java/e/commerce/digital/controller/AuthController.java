package e.commerce.digital.controller;

import e.commerce.digital.config.JwtService;
import e.commerce.digital.dto.auth.LoginRequest;
import e.commerce.digital.dto.auth.RegistroRequest;
import e.commerce.digital.dto.auth.RegistroVendedorRequest;
import e.commerce.digital.dto.usuario.UsuarioResponse;
import e.commerce.digital.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// El registro y el inicio de sesión son públicos; el resto de rutas aplica sus roles.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /** Registro de un cliente nuevo. */
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(req));
    }

    /** Solicita una cuenta vendedora; un administrador debe aprobarla para habilitar el login. */
    @PostMapping("/registro-vendedor")
    public ResponseEntity<UsuarioResponse> solicitarAltaVendedor(
            @Valid @RequestBody RegistroVendedorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.solicitarAltaVendedor(req));
    }

    /** Inicia una sesión HTTP para los siguientes pedidos del navegador. */
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(@Valid @RequestBody LoginRequest req,
                                                 HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        req.email().trim().toLowerCase(),
                        req.password()));
        response.addHeader(HttpHeaders.SET_COOKIE,
                jwtService.authenticationCookie(jwtService.createToken(authentication)).toString());
        return ResponseEntity.ok(usuarioService.obtenerPerfil(authentication.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, jwtService.expiredCookie().toString());
        return ResponseEntity.noContent().build();
    }
}
