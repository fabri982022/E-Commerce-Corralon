package e.commerce.digital.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String COOKIE_NAME = "AUTH_TOKEN";
    private static final String ISSUER = "corralon-central";
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "CLIENTE", "VENDEDOR");

    private final SecretKey jwtSecretKey;

    @Value("${app.jwt.expiration-minutes:60}")
    private long expirationMinutes;

    @Value("${app.auth.cookie-secure:false}")
    private boolean secureCookie;

    public String createToken(Authentication authentication) {
        Instant now = Instant.now();
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .filter(ALLOWED_ROLES::contains)
                .distinct()
                .toList();
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("La autenticación debe incluir un rol válido");
        }

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(expirationMinutes))))
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();
        SignedJWT token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).build(), claims);
        try {
            token.sign(new MACSigner(jwtSecretKey.getEncoded()));
            return token.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("No se pudo firmar el token de autenticación", ex);
        }
    }

    public JwtPrincipal validateToken(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidJwtException("Token JWT vacío");
        }

        try {
            SignedJWT token = SignedJWT.parse(value);
            if (!JWSAlgorithm.HS256.equals(token.getHeader().getAlgorithm())
                    || !token.verify(new MACVerifier(jwtSecretKey.getEncoded()))) {
                throw new InvalidJwtException("Firma JWT inválida");
            }

            JWTClaimsSet claims = token.getJWTClaimsSet();
            Date expiresAt = claims.getExpirationTime();
            String subject = claims.getSubject();
            List<String> roles = claims.getStringListClaim("roles");
            if (!ISSUER.equals(claims.getIssuer())
                    || expiresAt == null
                    || !expiresAt.after(new Date())
                    || subject == null
                    || subject.isBlank()
                    || roles == null
                    || roles.isEmpty()
                    || roles.stream().anyMatch(role -> !ALLOWED_ROLES.contains(role))) {
                throw new InvalidJwtException("Claims JWT inválidos o expirados");
            }
            return new JwtPrincipal(subject, roles);
        } catch (ParseException | JOSEException ex) {
            throw new InvalidJwtException("Token JWT mal formado", ex);
        }
    }

    public ResponseCookie authenticationCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(expirationMinutes))
                .build();
    }

    public ResponseCookie expiredCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public record JwtPrincipal(String subject, List<String> roles) {
        public JwtPrincipal {
            roles = List.copyOf(roles);
        }
    }

    public static class InvalidJwtException extends RuntimeException {
        public InvalidJwtException(String message) {
            super(message);
        }

        public InvalidJwtException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
