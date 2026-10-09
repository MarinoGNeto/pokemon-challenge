package com.marinogneto.pokemon.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * 401 and 403 happen in the security filter chain, before any controller, so the {@code @RestControllerAdvice}
 * never sees them. These handlers give them the same RFC 9457 body as every other error. Spring's Bearer
 * handlers still set the status and the RFC 6750 {@code WWW-Authenticate} challenge.
 */
final class ProblemDetailSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final AuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final AccessDeniedHandler bearerAccessDenied = new BearerTokenAccessDeniedHandler();
    private final JsonMapper json;

    ProblemDetailSecurityHandlers(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException, jakarta.servlet.ServletException {
        bearerEntryPoint.commence(request, response, e);
        if (e instanceof OAuth2AuthenticationException) {
            write(request, response, "invalid-token", "Invalid token",
                    "The access token is invalid or has expired. Sign in again.");
        } else {
            write(request, response, "unauthorized", "Authentication required",
                    "Sign in with POST /api/auth/login and send 'Authorization: Bearer <token>'.");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException, jakarta.servlet.ServletException {
        bearerAccessDenied.handle(request, response, e);
        write(request, response, "forbidden", "Access denied", "Your role does not allow this operation.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, String type, String title,
                       String detail) throws IOException {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "urn:pokemon-challenge:problem:" + type);
        problem.put("title", title);
        problem.put("status", response.getStatus());
        problem.put("detail", detail);
        problem.put("instance", request.getRequestURI());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(), problem);
    }
}
