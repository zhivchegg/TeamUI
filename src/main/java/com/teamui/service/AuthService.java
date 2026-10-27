package com.teamui.service;

import com.teamui.domain.enums.UserRole;
import com.teamui.domain.invite.Invite;
import com.teamui.domain.invite.InviteRepository;
import com.teamui.domain.membership.TeamMembership;
import com.teamui.domain.membership.TeamMembershipRepository;
import com.teamui.domain.user.User;
import com.teamui.domain.user.UserRepository;
import com.teamui.dto.LoginRequest;
import com.teamui.dto.LoginResponse;
import com.teamui.dto.RegisterRequest;
import com.teamui.security.jwt.JwtUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Authentication business logic: login, registration via invite tokens,
 * access-token renewal with refresh-token rotation, and logout.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final LeadershipResolver leadershipResolver;

    public AuthService(UserRepository userRepository,
                        InviteRepository inviteRepository,
                        TeamMembershipRepository teamMembershipRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtils jwtUtils,
                        RefreshTokenService refreshTokenService,
                        LeadershipResolver leadershipResolver) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.leadershipResolver = leadershipResolver;
    }

    /**
     * Result of a successful authentication.
     *
     * @param session      access token and user info, returned in the response body
     * @param refreshToken raw refresh token, returned to the client only as an httpOnly cookie
     */
    public record AuthResult(LoginResponse session, String refreshToken) {}

    /**
     * Authenticates a user by email and password.
     *
     * @param request login credentials
     * @return new session
     * @throws BadCredentialsException if credentials are invalid
     */
    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        return issueSession(user, resolvePrimaryRole(user));
    }

    /**
     * Completes self-registration using an invitation token.
     *
     * @param request registration details including invite token
     * @return new session for the newly created user
     * @throws IllegalArgumentException if token is invalid, expired, or already used
     */
    @Transactional
    public AuthResult register(RegisterRequest request) {
        Invite invite = inviteRepository.findByToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired invitation token"));

        if (invite.isExpired()) {
            throw new IllegalArgumentException("Invitation token has expired");
        }

        if (invite.isUsed()) {
            throw new IllegalArgumentException("Invitation token has already been used");
        }

        if (userRepository.existsByEmail(invite.getEmail())) {
            throw new IllegalArgumentException("User already exists with this email");
        }

        User user = new User();
        user.setEmail(invite.getEmail().trim().toLowerCase());
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user = userRepository.save(user);

        // Create team membership if invite specifies a team
        if (invite.getTeam() != null) {
            TeamMembership membership = new TeamMembership();
            membership.setUser(user);
            membership.setTeam(invite.getTeam());
            membership.setRole(invite.getRole());
            membership.setJoinedAt(Instant.now());
            teamMembershipRepository.save(membership);
        }

        // Mark invite as used
        invite.setUsedAt(Instant.now());
        inviteRepository.save(invite);

        return issueSession(user, invite.getRole().name());
    }

    /**
     * Exchanges a refresh token for a new access token and a new refresh token.
     *
     * <p>Deliberately not transactional: the refresh token is consumed in its own
     * transaction, so a reuse attempt that fails here still commits the revocation of all
     * the user's tokens.</p>
     *
     * @param rawRefreshToken the refresh token from the client cookie
     * @return new session with a rotated refresh token
     * @throws BadCredentialsException if the token is invalid or the account is deactivated
     */
    public AuthResult refresh(String rawRefreshToken) {
        UUID userId = refreshTokenService.consume(rawRefreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        return issueSession(user, resolvePrimaryRole(user));
    }

    /**
     * Revokes the refresh token of the current session.
     *
     * @param rawRefreshToken the refresh token from the client cookie, may be null
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthResult issueSession(User user, String role) {
        String accessToken = jwtUtils.generateToken(user.getId(), user.getEmail(), role);

        LoginResponse session = new LoginResponse(
                accessToken,
                "Bearer",
                user.getId(),
                user.getEmail(),
                role,
                jwtUtils.accessTokenTtlSeconds()
        );

        return new AuthResult(session, refreshTokenService.issue(user));
    }

    private String resolvePrimaryRole(User user) {
        var stream = leadershipResolver.find(user.getId());
        if (stream.isPresent()) {
            return stream.get().role().name();
        }
        var memberships = teamMembershipRepository.findAllByUserId(user.getId(), Pageable.ofSize(1));
        if (!memberships.isEmpty()) {
            return memberships.getContent().getFirst().getRole().name();
        }
        return UserRole.TEAM_MEMBER.name();
    }
}
