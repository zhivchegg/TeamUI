package com.teamui.web;

import com.teamui.dto.LoginRequest;
import com.teamui.dto.LoginResponse;
import com.teamui.dto.RegisterRequest;
import com.teamui.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints.
 *
 * <p>Supports login (JWT issuance) and self-registration via invitation tokens.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticate a user and return a JWT access token.
     *
     * @param request login credentials (email, password)
     * @return token info (JWT, user id, role, expiry)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Complete self-registration using an invitation token.
     *
     * @param request registration details (token, name, password)
     * @return token info for the newly created user
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody @Valid RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }
}
