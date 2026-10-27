package com.teamui.web;

import com.teamui.dto.ApiResponse;
import com.teamui.dto.LoginRequest;
import com.teamui.dto.LoginResponse;
import com.teamui.dto.RegisterRequest;
import com.teamui.service.AuthService;
import com.teamui.service.AuthService.AuthResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Public authentication endpoints.
 *
 * <p>Access tokens are returned in the response body. The refresh token is only ever sent
 * as an httpOnly cookie scoped to {@code /api/auth}, so frontend JavaScript cannot read it.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final Duration refreshLifetime;

    public AuthController(AuthService authService,
                          @Value("${teamui.jwt.refresh-expiration-days:14}") long refreshDays) {
        this.authService = authService;
        this.refreshLifetime = Duration.ofDays(refreshDays);
    }

    /**
     * Authenticate a user. Returns an access token and sets the refresh cookie.
     *
     * @param request login credentials (email, password)
     * @return session info with the access token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        return withSession(authService.login(request));
    }

    /**
     * Complete self-registration using an invitation token. Returns an access token and sets the refresh cookie.
     *
     * @param request registration details (token, name, password)
     * @return session info for the newly created user
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@RequestBody @Valid RegisterRequest request) {
        return withSession(authService.register(request));
    }

    /**
     * Issue a new access token using the refresh cookie. The refresh token is rotated:
     * the old one stops working and a new cookie is set.
     *
     * @param refreshToken value of the refresh cookie
     * @return session info with a new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return withSession(authService.refresh(refreshToken));
    }

    /**
     * Revoke the current refresh token and clear the cookie.
     *
     * @param refreshToken value of the refresh cookie
     * @return empty success envelope
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        ResponseCookie cleared = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .body(ApiResponse.success(null));
    }

    private ResponseEntity<ApiResponse<LoginResponse>> withSession(AuthResult result) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(refreshLifetime)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(result.session()));
    }
}
