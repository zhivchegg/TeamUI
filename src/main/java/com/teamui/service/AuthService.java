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

/**
 * Authentication business logic: login, registration via invite tokens.
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

    public AuthService(UserRepository userRepository,
                        InviteRepository inviteRepository,
                        TeamMembershipRepository teamMembershipRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Authenticates a user by email and password, issuing a JWT.
     *
     * @param request login credentials
     * @return token response with user info
     * @throws BadCredentialsException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        // Resolve primary role from memberships (first active role found)
        String role = resolvePrimaryRole(user);

        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), role);

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                role,
                86400L // 24 hours in seconds
        );
    }

    /**
     * Completes self-registration using an invitation token.
     *
     * @param request registration details including invite token
     * @return token response for the newly created user
     * @throws IllegalArgumentException if token is invalid, expired, or already used
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
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

        String role = invite.getRole().name();
        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), role);

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                role,
                86400L
        );
    }

    private String resolvePrimaryRole(User user) {
        var memberships = teamMembershipRepository.findAllByUserId(user.getId(), Pageable.ofSize(1));
        if (!memberships.isEmpty()) {
            return memberships.getContent().getFirst().getRole().name();
        }
        return UserRole.TEAM_MEMBER.name();
    }
}
