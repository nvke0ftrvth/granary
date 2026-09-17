package com.example.granary.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.granary.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    // Minimum decoded key length (bytes) required by jjwt for HS256.
    private static final int MIN_KEY_BYTES = 32;

    @Value("${app.jwt.secret}")
    private String secretKey;

    // Comma-separated, previously-active secrets accepted only when verifying tokens (never for signing).
    @Value("${app.jwt.secret.previous:}")
    private String previousSecretKeys;

    @Value("${app.jwt.expiration}")
    private long expirationMs;

    private SecretKey signingKey;
    private List<SecretKey> previousSigningKeys;

    @PostConstruct
    void init() {
        this.signingKey = buildKey("JWT_SECRET", secretKey, true);

        this.previousSigningKeys = new ArrayList<>();
        for (String candidate : previousSecretKeys.split(",")) {
            String trimmed = candidate.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            SecretKey key = buildKey("JWT_SECRET_PREVIOUS", trimmed, false);
            if (key != null) {
                previousSigningKeys.add(key);
            }
        }
        if (!previousSigningKeys.isEmpty()) {
            log.info("JWT key rotation active: {} previous secret(s) will still be accepted when verifying tokens",
                    previousSigningKeys.size());
        }
    }

    // -------------------------
    // Generate a token for a user
    // Returns: String (the JWT token)
    // -------------------------
    public String generateToken(User userDetails) {
        return generateToken(Map.of(), userDetails);
    }

    // Overload that accepts extra claims (e.g. roles, userId)
    // Returns: String (the JWT token with extra claims embedded)
    public String generateToken(Map<String, Object> extraClaims, User userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    // -------------------------
    // Validate a token against a user
    // Returns: boolean (true if token is valid and belongs to this user)
    // -------------------------
    public boolean isTokenValid(String token, User userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // -------------------------
    // Extract the username (subject) from a token
    // Returns: String (the username stored in the token subject claim)
    // -------------------------
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // -------------------------
    // Extract the expiration date from a token
    // Returns: Date (when the token expires)
    // -------------------------
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // -------------------------
    // Generic claim extractor — lets you pull any field from the token
    // Returns: T (whatever type the claim resolver returns)
    // -------------------------
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // -------------------------
    // Private helpers
    // -------------------------

    // Returns: boolean (true if the token expiration is before now)
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Returns: Claims (the full decoded payload of the token)
    private Claims extractAllClaims(String token) {
        try {
            return parseWithKey(token, signingKey);
        } catch (JwtException ex) {
            for (SecretKey previousKey : previousSigningKeys) {
                try {
                    return parseWithKey(token, previousKey);
                } catch (JwtException ignored) {
                    // try the next previous key
                }
            }
            throw ex;
        }
    }

    private Claims parseWithKey(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Decodes and validates a Base64 secret into a signing key. When required is true,
    // a misconfigured secret fails startup instead of surfacing later as a token-parsing error.
    private SecretKey buildKey(String sourceName, String secret, boolean required) {
        if (secret == null || secret.isBlank()) {
            if (required) {
                throw new IllegalStateException(
                        sourceName + " is missing or blank. Set the " + sourceName
                        + " environment variable to a Base64-encoded value of at least "
                        + MIN_KEY_BYTES + " bytes (256 bits), e.g. `openssl rand -base64 32`.");
            }
            return null;
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ex) {
            return failOrSkip(required, sourceName + " is not valid Base64: " + ex.getMessage(), ex);
        }

        if (keyBytes.length < MIN_KEY_BYTES) {
            return failOrSkip(required, String.format(
                    "%s decodes to %d bytes, but at least %d bytes (256 bits) are required.",
                    sourceName, keyBytes.length, MIN_KEY_BYTES), null);
        }

        try {
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException ex) {
            return failOrSkip(required, sourceName + " is too weak to sign tokens: " + ex.getMessage(), ex);
        }
    }

    private SecretKey failOrSkip(boolean required, String message, Exception cause) {
        if (required) {
            throw new IllegalStateException(message, cause);
        }
        log.warn("Ignoring invalid previous JWT secret: {}", message);
        return null;
    }
}