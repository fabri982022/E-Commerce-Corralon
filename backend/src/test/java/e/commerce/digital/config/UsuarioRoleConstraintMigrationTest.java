package e.commerce.digital.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UsuarioRoleConstraintMigrationTest {

    @Test
    void replacesLegacyRoleAndDiscriminatorChecksWithoutRemovingOtherChecks() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(
                Map.of("constraint_name", "usuarios_chk_1",
                        "check_clause", "(`rol` in (_utf8mb4'ADMIN',_utf8mb4'CLIENTE'))"),
                Map.of("constraint_name", "usuarios_chk_2",
                        "check_clause", "(`tipo_usuario` in (_utf8mb4'ADMIN',_utf8mb4'CLIENTE'))"),
                Map.of("constraint_name", "usuarios_chk_3",
                        "check_clause", "(`activo` in (0,1))")));
        UsuarioRoleConstraintMigration migration = new UsuarioRoleConstraintMigration(jdbcTemplate);

        migration.run(mock(ApplicationArguments.class));

        verify(jdbcTemplate).execute("ALTER TABLE usuarios ADD CONSTRAINT `chk_usuarios_rol_values` "
                + "CHECK (rol IN ('ADMIN', 'CLIENTE', 'VENDEDOR'))");
        verify(jdbcTemplate).execute("ALTER TABLE usuarios ADD CONSTRAINT `chk_usuarios_tipo_usuario_values` "
                + "CHECK (tipo_usuario IN ('ADMIN', 'CLIENTE', 'VENDEDOR'))");
        verify(jdbcTemplate).execute("ALTER TABLE usuarios DROP CHECK `usuarios_chk_1`");
        verify(jdbcTemplate).execute("ALTER TABLE usuarios DROP CHECK `usuarios_chk_2`");
        verify(jdbcTemplate, times(4)).execute(anyString());
    }

    @Test
    void leavesCurrentRoleCheckUnchanged() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(
                Map.of("constraint_name", "usuarios_chk_1",
                        "check_clause", "(`rol` in ('ADMIN','CLIENTE','VENDEDOR'))"),
                Map.of("constraint_name", "usuarios_chk_2",
                        "check_clause", "(`tipo_usuario` in ('ADMIN','CLIENTE','VENDEDOR'))")));
        UsuarioRoleConstraintMigration migration = new UsuarioRoleConstraintMigration(jdbcTemplate);

        migration.run(mock(ApplicationArguments.class));

        verify(jdbcTemplate, never()).execute(anyString());
    }
}
