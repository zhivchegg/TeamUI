package com.teamui.security.service;

import com.teamui.domain.user.User;
import com.teamui.domain.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads users by email for Spring Security authentication.
 *
 * <p>Role resolution is simplified to a single role per user for JWT payload.
 * In production, role resolution should consult {@link com.teamui.domain.membership.TeamMembership}.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // TODO: Resolve actual role from TeamMembership or admin flags
        return new UserDetailsImpl(user, "TEAM_MEMBER");
    }
}
