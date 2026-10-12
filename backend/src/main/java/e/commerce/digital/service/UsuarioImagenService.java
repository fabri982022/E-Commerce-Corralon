package e.commerce.digital.service;

import e.commerce.digital.dto.common.ImagenResponse;
import e.commerce.digital.dto.usuario.UsuarioResponse;
import e.commerce.digital.mapper.UsuarioMapper;
import e.commerce.digital.models.Usuario;
import e.commerce.digital.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UsuarioImagenService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final ImagenService imagenService;

    /** Sube o reemplaza la foto del usuario autenticado (email tomado de la sesión, nunca del cliente). */
    @Transactional
    public UsuarioResponse actualizarFoto(String email, MultipartFile archivo) {
        Usuario usuario = buscar(email);
        String publicIdAnterior = usuario.getFotoPublicId();

        ImagenResponse nueva = imagenService.subir(archivo, ImagenService.CARPETA_USUARIOS);
        usuario.setFotoUrl(nueva.url());
        usuario.setFotoPublicId(nueva.publicId());

        imagenService.limpiarAlFinalizarTransaccion(publicIdAnterior, nueva.publicId());
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse eliminarFoto(String email) {
        Usuario usuario = buscar(email);
        String publicIdAnterior = usuario.getFotoPublicId();

        usuario.setFotoUrl(null);
        usuario.setFotoPublicId(null);

        imagenService.limpiarAlFinalizarTransaccion(publicIdAnterior, null);
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    private Usuario buscar(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }
}
