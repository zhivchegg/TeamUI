package com.teamui.security.config;

import com.teamui.security.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration for the TeamUI API.
 *
 * <p>Stateless JWT authentication, role-based access control, CORS support.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authEntryPoint;

    @Value("${teamui.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                           UserDetailsService userDetailsService,
                           AuthenticationEntryPoint authEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.authEntryPoint = authEntryPoint;
    }

    /**
     * Configures the security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/invites/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // Role-based endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/stream/**").hasAnyRole("STREAM_LEAD", "STREAM_IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/meetings/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/meetings/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/meetings/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/events/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/events/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/competencies/scores/**/lead").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/competencies/scores/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/competencies/scores/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/systems/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/systems/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/systems/**").hasAnyRole("TEAM_LEAD", "IT_LEAD", "ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS configuration allowing the SPA front-end to communicate with the API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
