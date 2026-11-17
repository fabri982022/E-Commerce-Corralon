package e.commerce.digital.config;

import e.commerce.digital.models.Carrito;
import e.commerce.digital.models.Usuario;
import e.commerce.digital.models.UsuarioAdmin;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.UsuarioRepository;
import e.commerce.digital.util.enumerated.Rol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class InitialUsersSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedAccount admin1;
    private final SeedAccount admin2;
    private final SeedAccount client1;
    private final SeedAccount client2;

    public InitialUsersSeeder(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.admin1.email:}") String admin1Email,
            @Value("${app.seed.admin1.password:}") String admin1Password,
            @Value("${app.seed.admin2.email:}") String admin2Email,
            @Value("${app.seed.admin2.password:}") String admin2Password,
            @Value("${app.seed.client1.email:}") String client1Email,
            @Value("${app.seed.client1.password:}") String client1Password,
            @Value("${app.seed.client2.email:}") String client2Email,
            @Value("${app.seed.client2.password:}") String client2Password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.admin1 = new SeedAccount(admin1Email, admin1Password, Rol.ADMIN, "Administrador", "Principal");
        this.admin2 = new SeedAccount(admin2Email, admin2Password, Rol.ADMIN, "Administrador", "Operador");
        this.client1 = new SeedAccount(client1Email, client1Password, Rol.CLIENTE, "Cliente", "Demo");
        this.client2 = new SeedAccount(client2Email, client2Password, Rol.CLIENTE, "Cliente", "Prueba");
    }

    @Override
    @Transactional
    public void run(String... args) {
        createIfMissing(admin1);
        createIfMissing(admin2);
        createIfMissing(client1);
        createIfMissing(client2);
    }

    private void createIfMissing(SeedAccount account) {
        String email = account.email().trim().toLowerCase(Locale.ROOT);
        if (email.isBlank() || account.password().isBlank()) {
            throw new IllegalStateException("Configure email y contraseña para todas las cuentas iniciales");
        }
        if (account.password().getBytes(StandardCharsets.UTF_8).length < 8
                || account.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Las contraseñas de cuentas iniciales deben tener entre 8 y 72 bytes");
        }

        var existing = usuarioRepository.findByEmail(email);
        if (existing.isPresent()) {
            Usuario user = existing.get();
            if (user.getRol() != account.role()) {
                throw new IllegalStateException("El correo configurado para la cuenta inicial ya existe con otro rol: " + email);
            }
            log.info("La cuenta inicial {} ya existe; se conserva sin cambios", email);
            return;
        }

        Usuario user = account.role() == Rol.ADMIN ? new UsuarioAdmin() : new UsuarioCliente();
        user.setNombre(account.nombre());
        user.setApellido(account.apellido());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(account.password()));
        user.setActivo(true);

        if (user instanceof UsuarioCliente cliente) {
            Carrito carrito = new Carrito();
            carrito.setUsuario(cliente);
            cliente.setCarrito(carrito);
        }

        usuarioRepository.save(user);
        log.info("Se creó cuenta inicial {} con rol {}", email, account.role());
    }

    private record SeedAccount(String email, String password, Rol role, String nombre, String apellido) {
    }
}
