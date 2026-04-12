package com.automatization.comunications.config;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-API-Key";

    private static final Set<String> PUBLIC_PATTERNS = Set.of(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info"
    );

    private final PathMatcher pathMatcher = new AntPathMatcher();

    private final String apiKey;

    public ApiKeyFilter(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (apiKey == null || apiKey.isBlank()) {
            writeUnauthorized(response, "Server API key not configured");
            return;
        }

        String provided = request.getHeader(HEADER_NAME);
        if (provided == null || !constantTimeEquals(provided, apiKey)) {
            writeUnauthorized(response, "Invalid or missing " + HEADER_NAME + " header");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}"
        );
    }
}
