package greencity.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InternalApiKeyFilterTest {
    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    @Mock
    FilterChain filterChain;

    @InjectMocks
    private InternalApiKeyFilter internalApiKeyFilter;

    private final String validApiKey = "testApiKey";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(internalApiKeyFilter, "validApiKey", validApiKey);
    }

    @Test
    void shouldPassWhenApiKeyIsValid() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/email/notification/unregistered");
        when(request.getHeader("EmailApiKey")).thenReturn(validApiKey);

        internalApiKeyFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void shouldPassWhenApiKeyIsInvalid() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/email/notification/unregistered");
        when(request.getHeader("EmailApiKey")).thenReturn("InvalidApiKey");

        internalApiKeyFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
