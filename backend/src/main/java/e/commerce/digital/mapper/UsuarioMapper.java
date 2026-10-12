package e.commerce.digital.mapper;

import e.commerce.digital.dto.auth.RegistroRequest;
import e.commerce.digital.dto.usuario.UsuarioAdminRequest;
import e.commerce.digital.dto.usuario.UsuarioResponse;
import e.commerce.digital.dto.usuario.UsuarioUpdateRequest;
import e.commerce.digital.models.Usuario;
import e.commerce.digital.models.UsuarioAdmin;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.util.enumerated.Rol;
import org.springframework.stereotype.Component;

// El mapper no encripta: el service recibe el hash ya calculado (BCrypt) y se lo pasa.
@Component
public class UsuarioMapper {

    // Registro público: siempre crea un cliente
    public UsuarioCliente toCliente(RegistroRequest req, String passwordHash) {
        UsuarioCliente u = new UsuarioCliente();
        u.setNombre(req.nombre());
        u.setApellido(req.apellido());
        u.setEmail(req.email());
        u.setPasswordHash(passwordHash);
        u.setTelefono(req.telefono());
        u.setDireccion(req.direccion());
        u.setActivo(true);
        return u;
    }

    // Alta hecha por el administrador: instancia la subclase según el rol
    public Usuario toEntity(UsuarioAdminRequest req, String passwordHash) {
        Usuario u = req.rol() == Rol.ADMIN ? new UsuarioAdmin() : new UsuarioCliente();
        u.setNombre(req.nombre());
        u.setApellido(req.apellido());
        u.setEmail(req.email());
        u.setPasswordHash(passwordHash);
        u.setTelefono(req.telefono());
        u.setDireccion(req.direccion());
        u.setActivo(true);
        return u;
    }

    // Edición del propio perfil: no toca email, rol, activo ni contraseña
    public void updateEntity(Usuario u, UsuarioUpdateRequest req) {
        u.setNombre(req.nombre());
        u.setApellido(req.apellido());
        u.setTelefono(req.telefono());
        u.setDireccion(req.direccion());
    }

    public UsuarioResponse toResponse(Usuario u) {
        return UsuarioResponse.desde(u);
    }
}
