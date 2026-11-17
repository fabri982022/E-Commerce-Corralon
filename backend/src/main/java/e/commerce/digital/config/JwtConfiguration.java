package e.commerce.digital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfiguration {

    @Bean
    SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String encodedSecret) {
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET debe ser una clave Base64 válida de al menos 32 bytes", ex);
        }
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

}
