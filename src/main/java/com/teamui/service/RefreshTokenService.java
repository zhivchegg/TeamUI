package com.teamui.service;

import com.teamui.domain.auth.RefreshToken;
import com.teamui.domain.auth.RefreshTokenRepository;
import com.teamui.domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Lifecycle of refresh tokens: issue, single-use rotation, revocation.
 *
 * <p>The raw token is a random 256-bit value returned to the client once; only its
 * SHA-256 hash is stored. Presenting an already-revoked token is treated as theft:
 * every active token of that user is revoked, which forces a new login.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final long lifetimeDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${teamui.jwt.refresh-expiration-days:14}") long lifetimeDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.lifetimeDays = lifetimeDays;
    }

    /**
     * Creates a refresh token for the user.
     *
     * @param user the token owner
     * @return the raw token to hand to the client; it is not stored anywhere in plain text
     */
    @Transactional
    public String issue(User user) {
        String rawToken = newRawToken();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(lifetimeDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(token);

        return rawToken;
    }

    /**
     * Validates a refresh token and revokes it (single use).
     *
     * <p>{@code noRollbackFor} keeps the revocation of all user tokens on reuse
     * even though the call fails.</p>
     *
     * @param rawToken the token from the client cookie
     * @return id of the token owner
     * @throws BadCredentialsException if the token is missing, unknown, expired or reused
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public UUID consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is missing");
        }

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        UUID userId = stored.getUser().getId();

        if (stored.getRevokedAt() != null) {
            revokeAllForUser(userId);
            throw new BadCredentialsException("Refresh token reuse detected");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }

        stored.setRevokedAt(Instant.now());
        return userId;
    }

    /**
     * Revokes a refresh token on logout. Unknown or missing tokens are ignored.
     *
     * @param rawToken the token from the client cookie
     */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private void revokeAllForUser(UUID userId) {
        Instant now = Instant.now();
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(token -> token.setRevokedAt(now));
    }

    private String newRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
