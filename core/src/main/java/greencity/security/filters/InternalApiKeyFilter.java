package greencity.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;

/**
 * Filter that validates internal API key for protected endpoints. Intercepts
 * requests to {@code /email/notification/unregistered} and verifies that they
 * contain a valid API key in the request header. If the key is missing or
 * invalid, request processing is terminated with HTTP 403 (Forbidden) status.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {
    @Value("${internal.api.key}")
    private String validApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if (requestURI.equals("/email/notification/unregistered")) {
            String apiKey = request.getHeader("EmailApiKey");

            if (apiKey == null || !apiKey.equals(validApiKey)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}