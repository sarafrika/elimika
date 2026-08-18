package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserSyncFilterTest {

    @Mock
    private UserManagementService userManagementService;

    @Mock
    private FilterChain filterChain;

    private UserSyncFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserSyncFilter(userManagementService);
        ReflectionTestUtils.setField(filter, "realm", "elimika");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void skipsServiceAccountTokens() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt(
                "service-account-elimika-api",
                "client-service-account"
        ), List.of()));
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(userManagementService, never()).ensureUserExists(anyString(), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void syncsRegularUserTokens() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt(
                "sarafika@example.com",
                "user-123"
        ), List.of()));

        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(userManagementService).ensureUserExists("user-123", "elimika");
        verify(filterChain).doFilter(request, response);
    }

    private Jwt jwt(String preferredUsername, String subject) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .subject(subject)
                .claim("preferred_username", preferredUsername)
                .build();
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/api/v1/profile");
    }
}
