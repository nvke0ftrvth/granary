package com.example.granary.business;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.granary.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration}")
    private long expirationMs;

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
                .signWith(getSigningKey())
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
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Returns: SecretKey (the signing key derived from your secret string)
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}