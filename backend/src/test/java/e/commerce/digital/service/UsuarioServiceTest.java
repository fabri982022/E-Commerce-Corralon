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
import e.commerce.digital.models.Usuario;
import e.commerce.digital.models.UsuarioAdmin;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.UsuarioRepository;
import e.commerce.digital.testutil.DatosDePrueba;
import e.commerce.digital.util.enumerated.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, new UsuarioMapper(), passwordEncoder);
    }

    // ----- Registro público -----

    @Test
    @DisplayName("registrar: crea un cliente con la contraseña encriptada, el email normalizado y su carrito")
    void registrar_ok() {
        when(usuarioRepository.existsByEmail("ana@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Secreta123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse r = service.registrar(new RegistroRequest(
                "Ana", "Pérez", "  ANA@Mail.com ", "Secreta123", "3884000000", "Calle 1"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();

        assertThat(guardado).isInstanceOf(UsuarioCliente.class);
        assertThat(guardado.getPasswordHash()).isEqualTo("hash-bcrypt").isNotEqualTo("Secreta123");
        assertThat(guardado.getEmail()).isEqualTo("ana@mail.com");
        UsuarioCliente cliente = (UsuarioCliente) guardado;
        assertThat(cliente.getCarrito()).isNotNull();
        assertThat(cliente.getCarrito().getUsuario()).isSameAs(cliente);

        assertThat(r.email()).isEqualTo("ana@mail.com");
        assertThat(r.rol()).isEqualTo(Rol.CLIENTE);
        assertThat(r.activo()).isTrue();
    }

    @Test
    @DisplayName("registrar: rechaza un email repetido sin encriptar ni guardar nada")
    void registrar_emailDuplicado() {
        when(usuarioRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new RegistroRequest(
                "Ana", "Pérez", "ana@mail.com", "Secreta123", null, null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("correo");

        verifyNoInteractions(passwordEncoder);
        verify(usuarioRepository, never()).save(any());
    }

    // ----- Alta por administrador -----

    @Test
    @DisplayName("crearPorAdmin: con rol ADMIN crea un administrador sin carrito")
    void crearPorAdmin_admin() {
        when(usuarioRepository.existsByEmail("luis@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Secreta123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse r = service.crearPorAdmin(new UsuarioAdminRequest(
                "Luis", "Gómez", "Luis@Mail.com", "Secreta123", null, null, Rol.ADMIN));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(UsuarioAdmin.class);
        assertThat(r.rol()).isEqualTo(Rol.ADMIN);
        assertThat(r.email()).isEqualTo("luis@mail.com");
    }

    @Test
    @DisplayName("crearPorAdmin: con rol CLIENTE crea un cliente con su carrito")
    void crearPorAdmin_cliente() {
        when(usuarioRepository.existsByEmail("pedro@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Secreta123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse r = service.crearPorAdmin(new UsuarioAdminRequest(
                "Pedro", "Díaz", "pedro@mail.com", "Secreta123", null, null, Rol.CLIENTE));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(UsuarioCliente.class);
        assertThat(((UsuarioCliente) captor.getValue()).getCarrito()).isNotNull();
        assertThat(r.rol()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    @DisplayName("crearPorAdmin: rechaza un email repetido")
    void crearPorAdmin_emailDuplicado() {
        when(usuarioRepository.existsByEmail("luis@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.crearPorAdmin(new UsuarioAdminRequest(
                "Luis", "Gómez", "luis@mail.com", "Secreta123", null, null, Rol.ADMIN)))
                .isInstanceOf(ConflictoException.class);

        verifyNoInteractions(passwordEncoder);
    }

    // ----- Perfil -----

    @Test
    @DisplayName("obtenerPerfil: devuelve el usuario sin exponer la contraseña")
    void obtenerPerfil_ok() {
        when(usuarioRepository.findByEmail("ana@mail.com"))
                .thenReturn(Optional.of(DatosDePrueba.cliente(1L, "ana@mail.com")));

        UsuarioResponse r = service.obtenerPerfil("ana@mail.com");

        assertThat(r.idUsuario()).isEqualTo(1L);
        assertThat(r.nombre()).isEqualTo("Ana");
        assertThat(r.toString()).doesNotContain("hash");
    }

    @Test
    @DisplayName("obtenerPerfil: un usuario inexistente responde no encontrado")
    void obtenerPerfil_noExiste() {
        when(usuarioRepository.findByEmail("nadie@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPerfil("nadie@mail.com"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("actualizarPerfil: cambia nombre y contacto, pero no el email ni el rol")
    void actualizarPerfil_ok() {
        UsuarioCliente ana = DatosDePrueba.cliente(1L, "ana@mail.com");
        ana.setRol(Rol.CLIENTE);
        when(usuarioRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(ana));

        UsuarioResponse r = service.actualizarPerfil("ana@mail.com",
                new UsuarioUpdateRequest("Anita", "López", "3884111111", "Calle 2"));

        assertThat(r.nombre()).isEqualTo("Anita");
        assertThat(r.apellido()).isEqualTo("López");
        assertThat(r.telefono()).isEqualTo("3884111111");
        assertThat(r.direccion()).isEqualTo("Calle 2");
        assertThat(r.email()).isEqualTo("ana@mail.com");
        assertThat(r.rol()).isEqualTo(Rol.CLIENTE);
    }

    // ----- Administración -----

    @Test
    @DisplayName("listar: sin rol devuelve todos los usuarios")
    void listar_sinRol() {
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(
                DatosDePrueba.cliente(1L, "ana@mail.com"), DatosDePrueba.admin(2L, "luis@mail.com"))));

        PageResponse<UsuarioResponse> r = service.listar(null, pageable);

        assertThat(r.contenido()).hasSize(2);
        verify(usuarioRepository, never()).findByRol(any(), any());
    }

    @Test
    @DisplayName("listar: con rol filtra por ese rol")
    void listar_conRol() {
        Pageable pageable = PageRequest.of(0, 10);
        UsuarioAdmin luis = DatosDePrueba.admin(2L, "luis@mail.com");
        luis.setRol(Rol.ADMIN);
        when(usuarioRepository.findByRol(Rol.ADMIN, pageable)).thenReturn(new PageImpl<>(List.of(luis)));

        PageResponse<UsuarioResponse> r = service.listar(Rol.ADMIN, pageable);

        assertThat(r.contenido()).extracting(UsuarioResponse::rol).containsExactly(Rol.ADMIN);
    }

    @Test
    @DisplayName("obtenerPorId: un usuario inexistente responde no encontrado")
    void obtenerPorId_noExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("cambiarEstado: el administrador desactiva a otro usuario")
    void cambiarEstado_otroUsuario() {
        UsuarioCliente ana = DatosDePrueba.cliente(1L, "ana@mail.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));

        UsuarioResponse r = service.cambiarEstado(1L, new UsuarioEstadoRequest(false), "luis@mail.com");

        assertThat(r.activo()).isFalse();
        assertThat(ana.getActivo()).isFalse();
    }

    @Test
    @DisplayName("cambiarEstado: un administrador no puede desactivar su propia cuenta")
    void cambiarEstado_propiaCuenta() {
        UsuarioAdmin luis = DatosDePrueba.admin(2L, "luis@mail.com");
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(luis));

        assertThatThrownBy(() -> service.cambiarEstado(2L, new UsuarioEstadoRequest(false), "LUIS@mail.com"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("propia cuenta");

        assertThat(luis.getActivo()).isTrue();
    }

    @Test
    @DisplayName("cambiarEstado: un usuario inexistente responde no encontrado")
    void cambiarEstado_noExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(99L, new UsuarioEstadoRequest(true), "luis@mail.com"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
