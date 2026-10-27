package com.teamui.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamui.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a JSON 403 for authenticated users who lack the required role.
 *
 * <p>Writes the response directly instead of calling {@code sendError}. With {@code sendError}
 * Spring re-dispatches the request to {@code /error}. The JWT filter skips error dispatches,
 * so that request is anonymous and the entry point overwrites 403 with 401.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Component
public class AccessDeniedHandlerJwt implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        new ObjectMapper().writeValue(response.getOutputStream(),
                ApiResponse.error(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to access this resource"));
    }
}
