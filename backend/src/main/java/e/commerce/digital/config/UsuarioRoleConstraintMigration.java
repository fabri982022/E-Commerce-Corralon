package e.commerce.digital.config;

import e.commerce.digital.util.enumerated.Rol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UsuarioRoleConstraintMigration implements ApplicationRunner {

    private static final String CHECKS_QUERY = """
            SELECT tc.CONSTRAINT_NAME AS constraint_name,
                   cc.CHECK_CLAUSE AS check_clause
            FROM information_schema.TABLE_CONSTRAINTS tc
            JOIN information_schema.CHECK_CONSTRAINTS cc
              ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
             AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
            WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
              AND tc.TABLE_NAME = 'usuarios'
              AND tc.CONSTRAINT_TYPE = 'CHECK'
            """;
    private static final Pattern ROLE_ENUM_CHECK = Pattern.compile("(?is)`?(rol|tipo_usuario)`?\\s+in\\s*\\(");
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbcTemplate;

    public UsuarioRoleConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> checks = jdbcTemplate.queryForList(CHECKS_QUERY);
        Map<String, List<Map<String, Object>>> staleChecksByColumn = new LinkedHashMap<>();
        for (Map<String, Object> check : checks) {
            String column = checkedColumn(check);
            if (column != null && !allowsVendedor(check)) {
                staleChecksByColumn.computeIfAbsent(column, ignored -> new ArrayList<>()).add(check);
            }
        }

        if (staleChecksByColumn.isEmpty()) {
            return;
        }

        Set<String> existingNames = checks.stream()
                .map(check -> (String) check.get("constraint_name"))
                .collect(Collectors.toSet());
        String roles = java.util.Arrays.stream(Rol.values())
                .map(role -> "'" + role.name() + "'")
                .collect(Collectors.joining(", "));

        for (String column : staleChecksByColumn.keySet()) {
            boolean currentCheckExists = checks.stream()
                    .anyMatch(check -> column.equals(checkedColumn(check)) && allowsVendedor(check));
            if (!currentCheckExists) {
                String replacementName = uniqueConstraintName(existingNames, column);
                jdbcTemplate.execute("ALTER TABLE usuarios ADD CONSTRAINT `" + replacementName
                        + "` CHECK (" + column + " IN (" + roles + "))");
                existingNames.add(replacementName);
            }
        }

        List<String> removedNames = new ArrayList<>();
        for (List<Map<String, Object>> staleChecks : staleChecksByColumn.values()) {
            for (Map<String, Object> staleCheck : staleChecks) {
                String constraintName = (String) staleCheck.get("constraint_name");
                jdbcTemplate.execute("ALTER TABLE usuarios DROP CHECK `" + safeIdentifier(constraintName) + "`");
                removedNames.add(constraintName);
            }
        }
        log.info("Migrated stale usuarios role/discriminator checks {} to allow current roles", removedNames);
    }

    private String checkedColumn(Map<String, Object> check) {
        Object clause = check.get("check_clause");
        if (!(clause instanceof String value)) {
            return null;
        }
        var matcher = ROLE_ENUM_CHECK.matcher(value);
        return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : null;
    }

    private boolean allowsVendedor(Map<String, Object> check) {
        String clause = (String) check.get("check_clause");
        return clause.toUpperCase(Locale.ROOT).contains(Rol.VENDEDOR.name());
    }

    private String uniqueConstraintName(Set<String> existingNames, String column) {
        String name = "chk_usuarios_" + column + "_values";
        int suffix = 2;
        while (existingNames.contains(name)) {
            name = "chk_usuarios_" + column + "_values_" + suffix++;
        }
        return name;
    }

    private String safeIdentifier(String identifier) {
        if (identifier == null || !IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalStateException("Unexpected usuarios check constraint name");
        }
        return identifier;
    }
}
