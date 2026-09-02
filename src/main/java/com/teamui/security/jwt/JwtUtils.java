package com.teamui.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Utility for generating and validating JWT tokens.
 *
 * <p>Uses JJWT 0.12.x API with {@code SecretKey} for HMAC-SHA-256 signing.
 * The secret must be at least 32 characters for HS-256.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Component
public class JwtUtils {

    @Value("${teamui.jwt.secret:ChangeThisSecretInProduction32CharsMin}")
    private String jwtSecret;

    @Value("${teamui.jwt.expiration-hours:24}")
    private int jwtExpirationHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT for the given user.
     *
     * @param userId the user's UUID
     * @param email  the user's email
     * @param role   the user's primary role (without {@code ROLE_} prefix)
     * @return compact JWT string
     */
    public String generateToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtExpirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key())
                .compact();
    }

    /**
     * Extracts the user UUID string from the token subject.
     */
    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the email claim.
     */
    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    /**
     * Extracts the role claim.
     */
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Validates token signature and expiration.
     *
     * @param token the JWT string
     * @return true if the token is valid and not expired
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.before(Date.from(Instant.now()));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
