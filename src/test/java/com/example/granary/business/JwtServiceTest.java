package com.example.granary.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.granary.model.User;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static String randomBase64Secret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static JwtService newService(String secret, String previousSecrets) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        ReflectionTestUtils.setField(service, "previousSecretKeys", previousSecrets);
        ReflectionTestUtils.setField(service, "expirationMs", 86_400_000L);
        service.init();
        return service;
    }

    @Test
    void init_missingSecret_throwsIllegalStateException() {
        assertThatThrownBy(() -> newService("", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is missing or blank");
    }

    @Test
    void init_secretTooShort_throwsIllegalStateException() {
        String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> newService(shortSecret, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void extractUsername_tokenSignedWithPreviousSecret_stillValidatesDuringRotation() {
        String oldSecret = randomBase64Secret();
        JwtService beforeRotation = newService(oldSecret, "");
        String token = beforeRotation.generateToken(new User("alice", "alice@test.com", "hash"));

        JwtService afterRotation = newService(randomBase64Secret(), oldSecret);

        assertThat(afterRotation.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void extractUsername_tokenSignedWithUnrecognizedSecret_throws() {
        JwtService beforeRotation = newService(randomBase64Secret(), "");
        String token = beforeRotation.generateToken(new User("alice", "alice@test.com", "hash"));

        JwtService afterRotation = newService(randomBase64Secret(), "");

        assertThatThrownBy(() -> afterRotation.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void init_invalidPreviousSecret_isIgnoredRatherThanFatal() {
        JwtService service = newService(randomBase64Secret(), "not-valid-base64!!!");
        String token = service.generateToken(new User("alice", "alice@test.com", "hash"));

        assertThat(service.extractUsername(token)).isEqualTo("alice");
    }
}
