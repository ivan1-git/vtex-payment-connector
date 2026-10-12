package dev.ivanjimenez.connector.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * VTEX manda en cada llamada las credenciales que el comercio configuró en la afiliación.
 * Si no coinciden, cortamos aquí antes de que llegue al controlador.
 */
@Component
public class VtexCredentialsFilter extends OncePerRequestFilter {

    public static final String APP_KEY_HEADER = "X-VTEX-API-AppKey";
    public static final String APP_TOKEN_HEADER = "X-VTEX-API-AppToken";

    private final ConnectorProperties properties;

    public VtexCredentialsFilter(ConnectorProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/payments");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean valid = matches(properties.appKey(), request.getHeader(APP_KEY_HEADER))
                && matches(properties.appToken(), request.getHeader(APP_TOKEN_HEADER));

        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"unauthorized\",\"message\":\"Credenciales de VTEX inválidas\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    // Comparación en tiempo constante para no dar pistas del token por timing
    private static boolean matches(String expected, String received) {
        if (expected == null || received == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
    }
}
