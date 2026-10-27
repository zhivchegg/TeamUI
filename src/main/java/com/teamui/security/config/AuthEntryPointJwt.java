package com.teamui.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamui.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom entry point that returns JSON error responses for unauthenticated requests.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        new ObjectMapper().writeValue(response.getOutputStream(),
                ApiResponse.error(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage()));
    }
}
