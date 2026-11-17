package e.commerce.digital.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        key = new SecretKeySpec(
                Base64.getDecoder().decode("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="),
                "HmacSHA256");
        jwtService = new JwtService(key);
        ReflectionTestUtils.setField(jwtService, "expirationMinutes", 60L);
        ReflectionTestUtils.setField(jwtService, "secureCookie", false);
    }

    @Test
    void tokenIncludesUserAndRoleAndCookieIsHttpOnly() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@corralon.local",
                "password",
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("FACTOR_PASSWORD")));

        var token = jwtService.createToken(authentication);
        var decoded = jwtService.validateToken(token);
        var signed = SignedJWT.parse(token);
        var cookie = jwtService.authenticationCookie(token).toString();

        assertThat(signed.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
        assertThat(signed.verify(new MACVerifier(key.getEncoded()))).isTrue();
        assertThat(decoded.subject()).isEqualTo("admin@corralon.local");
        assertThat(decoded.roles()).containsExactly("ADMIN");
        assertThat(cookie).contains("HttpOnly", "SameSite=Lax", "Path=/");
        assertThat(jwtService.expiredCookie().toString()).contains("Max-Age=0");
    }

    @Test
    void tokenAcceptsSellerRole() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "vendedor@corralon.local",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_VENDEDOR")));

        assertThat(jwtService.validateToken(jwtService.createToken(authentication)).roles())
                .containsExactly("VENDEDOR");
    }

    @Test
    void rejectsMalformedAndEmptyTokens() {
        assertThatThrownBy(() -> jwtService.validateToken(null))
                .isInstanceOf(JwtService.InvalidJwtException.class);
        assertThatThrownBy(() -> jwtService.validateToken("  "))
                .isInstanceOf(JwtService.InvalidJwtException.class);
        assertThatThrownBy(() -> jwtService.validateToken("not-a-jwt"))
                .isInstanceOf(JwtService.InvalidJwtException.class);
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() throws Exception {
        SecretKey otherKey = new SecretKeySpec(new byte[32], "HmacSHA256");
        String token = createSignedToken(otherKey, "corralon-central",
                "admin@corralon.local", Date.from(Instant.now().plusSeconds(60)), List.of("ADMIN"));

        assertThatThrownBy(() -> jwtService.validateToken(token))
                .isInstanceOf(JwtService.InvalidJwtException.class);
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String token = createSignedToken(key, "corralon-central",
                "admin@corralon.local", Date.from(Instant.now().minusSeconds(1)), List.of("ADMIN"));

        assertThatThrownBy(() -> jwtService.validateToken(token))
                .isInstanceOf(JwtService.InvalidJwtException.class);
    }

    @Test
    void rejectsTokensWithUnexpectedIssuerOrRole() throws Exception {
        String wrongIssuer = createSignedToken(key, "unexpected-issuer",
                "admin@corralon.local", Date.from(Instant.now().plusSeconds(60)), List.of("ADMIN"));
        String wrongRole = createSignedToken(key, "corralon-central",
                "admin@corralon.local", Date.from(Instant.now().plusSeconds(60)), List.of("ROLE_ADMIN"));

        assertThatThrownBy(() -> jwtService.validateToken(wrongIssuer))
                .isInstanceOf(JwtService.InvalidJwtException.class);
        assertThatThrownBy(() -> jwtService.validateToken(wrongRole))
                .isInstanceOf(JwtService.InvalidJwtException.class);
    }

    @Test
    void refusesToCreateTokenWithoutAnApplicationRole() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@corralon.local",
                "password",
                List.of(new SimpleGrantedAuthority("FACTOR_PASSWORD")));

        assertThatThrownBy(() -> jwtService.createToken(authentication))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String createSignedToken(SecretKey signingKey, String issuer, String subject,
                                    Date expiresAt, List<String> roles) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(expiresAt)
                .subject(subject)
                .claim("roles", roles)
                .build();
        SignedJWT token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        token.sign(new MACSigner(signingKey.getEncoded()));
        return token.serialize();
    }
}
