package e.commerce.digital.config;

import e.commerce.digital.models.Usuario;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.repository.UsuarioRepository;
import e.commerce.digital.util.enumerated.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InitialUsersSeederTest {

    @Test
    void createsTwoAdminsAndTwoClientsWithHashedPasswordsAndClientCarts() throws Exception {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repository.findByEmail(any())).thenReturn(Optional.empty());
        when(encoder.encode(any())).thenReturn("bcrypt-hash");
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        InitialUsersSeeder seeder = new InitialUsersSeeder(
                repository,
                encoder,
                "admin1@example.test", "admin-password-1",
                "admin2@example.test", "admin-password-2",
                "client1@example.test", "client-password-1",
                "client2@example.test", "client-password-2");

        seeder.run();

        var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(repository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).filteredOn(user -> user.getRol() == Rol.ADMIN).hasSize(2);
        assertThat(captor.getAllValues()).filteredOn(user -> user.getRol() == Rol.CLIENTE)
                .hasSize(2)
                .allSatisfy(user -> {
                    assertThat(user).isInstanceOf(UsuarioCliente.class);
                    assertThat(((UsuarioCliente) user).getCarrito()).isNotNull();
                    assertThat(user.getPasswordHash()).isEqualTo("bcrypt-hash");
                });
    }
}
