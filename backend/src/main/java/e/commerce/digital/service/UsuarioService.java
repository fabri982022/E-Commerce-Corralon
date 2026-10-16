package e.commerce.digital.service;

import e.commerce.digital.dto.auth.RegistroRequest;
import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.usuario.UsuarioAdminRequest;
import e.commerce.digital.dto.usuario.UsuarioEstadoRequest;
import e.commerce.digital.dto.usuario.UsuarioResponse;
import e.commerce.digital.dto.usuario.UsuarioUpdateRequest;
import e.commerce.digital.exception.ConflictoException;
import e.commerce.digital.exception.OperacionInvalidaException;
import e.commerce.digital.exception.RecursoNoEncontradoException;
import e.commerce.digital.mapper.UsuarioMapper;
import e.commerce.digital.models.Carrito;
import e.commerce.digital.models.Usuario;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.UsuarioRepository;
import e.commerce.digital.util.enumerated.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    /** Registro público: siempre crea un cliente con su carrito vacío. */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest req) {
        String email = normalizar(req.email());
        validarEmailLibre(email);

        UsuarioCliente cliente = usuarioMapper.toCliente(req, passwordEncoder.encode(req.password()));
        cliente.setEmail(email);
        asignarCarrito(cliente);

        return usuarioMapper.toResponse(usuarioRepository.save(cliente));
    }

    /**
     * Alta hecha por un administrador: crea un cliente o un administrador según el
     * rol.
     */
    @Transactional
    public UsuarioResponse crearPorAdmin(UsuarioAdminRequest req) {
        String email = normalizar(req.email());
        validarEmailLibre(email);

        Usuario usuario = usuarioMapper.toEntity(req, passwordEncoder.encode(req.password()));
        usuario.setEmail(email);
        if (usuario instanceof UsuarioCliente cliente) {
            asignarCarrito(cliente);
        }

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    public UsuarioResponse obtenerPerfil(String email) {
        return usuarioMapper.toResponse(buscarPorEmail(email));
    }

    @Transactional
    public UsuarioResponse actualizarPerfil(String email, UsuarioUpdateRequest req) {
        Usuario usuario = buscarPorEmail(email);
        usuarioMapper.updateEntity(usuario, req);
        return usuarioMapper.toResponse(usuario);
    }

    // ----- Administración -----

    public PageResponse<UsuarioResponse> listar(Rol rol, Pageable pageable) {
        var pagina = (rol == null)
                ? usuarioRepository.findAll(pageable)
                : usuarioRepository.findByRol(rol, pageable);
        return PageResponse.desde(pagina.map(usuarioMapper::toResponse));
    }

    public UsuarioResponse obtenerPorId(Long idUsuario) {
        return usuarioMapper.toResponse(buscarPorId(idUsuario));
    }

    /**
     * Activa o desactiva un usuario. Un administrador no puede desactivar su propia
     * cuenta.
     */
    @Transactional
    public UsuarioResponse cambiarEstado(Long idUsuario, UsuarioEstadoRequest req, String emailAdminActual) {
        Usuario usuario = buscarPorId(idUsuario);
        if (!req.activo() && usuario.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new OperacionInvalidaException("No puede desactivar su propia cuenta");
        }
        usuario.setActivo(req.activo());
        return usuarioMapper.toResponse(usuario);
    }

    // ----- Auxiliares -----

    // Un cliente nace con su carrito; la relación tiene cascade desde
    // UsuarioCliente
    private void asignarCarrito(UsuarioCliente cliente) {
        Carrito carrito = new Carrito();
        carrito.setUsuario(cliente);
        cliente.setCarrito(carrito);
    }

    private void validarEmailLibre(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("Ya existe un usuario con ese correo electrónico");
        }
    }

    // El email se guarda en minúsculas y sin espacios para que la unicidad no
    // dependa de mayúsculas
    private String normalizar(String email) {
        return email.trim().toLowerCase();
    }

    private Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private Usuario buscarPorId(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}