package com.teamui.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
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

    private static final int MIN_SECRET_BYTES = 32;

    /** Placeholder that used to be the default; a deployment that still has it is not secure. */
    private static final String FORMER_DEFAULT_SECRET = "ChangeThisSecretInProduction32CharsMin";

    @Value("${teamui.jwt.secret}")
    private String jwtSecret;

    @Value("${teamui.jwt.access-expiration-minutes:15}")
    private int accessExpirationMinutes;

    /**
     * Fails startup when the secret is missing, too short for HS-256, or the old placeholder,
     * instead of failing on the first login.
     *
     * @throws IllegalStateException if the secret is unsafe to use
     */
    @PostConstruct
    void validateSecret() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 bytes long");
        }
        if (FORMER_DEFAULT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET is the former placeholder value; set a new secret");
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access token lifetime in seconds, as reported to the client in {@code expiresIn}.
     *
     * @return lifetime in seconds
     */
    public long accessTokenTtlSeconds() {
        return accessExpirationMinutes * 60L;
    }

    /**
     * Generates a signed short-lived access JWT for the given user.
     *
     * @param userId the user's UUID
     * @param email  the user's email
     * @param role   the user's primary role (without {@code ROLE_} prefix)
     * @return compact JWT string
     */
    public String generateToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessExpirationMinutes, ChronoUnit.MINUTES);

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
