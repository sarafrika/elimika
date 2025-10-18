package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSyncFilter Tests")
class UserSyncFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private JwtAuthenticationToken jwtAuthenticationToken;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private UserSyncFilter userSyncFilter;

    private static final String TEST_REALM = "test-realm";
    private static final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";
    private static final String TEST_USERNAME = "test@example.com";
    private static final String TEST_PATH = "/api/v1/test";

    @BeforeEach
    void setUp() {
        // Set the realm value using ReflectionTestUtils
        ReflectionTestUtils.setField(userSyncFilter, "realm", TEST_REALM);

        // Clear SecurityContext before each test
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Filter Processing Tests")
    class FilterProcessingTests {

        @Test
        @DisplayName("Should skip processing for actuator endpoints")
        void shouldSkipActuatorEndpoints() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn("/actuator/health");

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userRepository, keycloakUserService, userService);
        }

        @Test
        @DisplayName("Should skip processing for swagger endpoints")
        void shouldSkipSwaggerEndpoints() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userRepository, keycloakUserService, userService);
        }

        @Test
        @DisplayName("Should skip processing for api-docs endpoints")
        void shouldSkipApiDocsEndpoints() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn("/v3/api-docs");

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userRepository, keycloakUserService, userService);
        }

        @Test
        @DisplayName("Should skip processing when no authentication present")
        void shouldSkipWhenNoAuthentication() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn(TEST_PATH);
            SecurityContextHolder.setContext(securityContext);
            when(securityContext.getAuthentication()).thenReturn(null);

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userRepository, keycloakUserService, userService);
        }

        @Test
        @DisplayName("Should skip processing when authentication is not JWT")
        void shouldSkipWhenNotJwtAuthentication() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn(TEST_PATH);
            Authentication otherAuth = mock(Authentication.class);
            SecurityContextHolder.setContext(securityContext);
            when(securityContext.getAuthentication()).thenReturn(otherAuth);

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userRepository, keycloakUserService, userService);
        }

        @Test
        @DisplayName("Should skip processing when JWT has no sub claim")
        void shouldSkipWhenNoSubClaim() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn(TEST_PATH);
            SecurityContextHolder.setContext(securityContext);
            when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
            when(jwtAuthenticationToken.isAuthenticated()).thenReturn(true);
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
            when(jwt.getClaimAsString("sub")).thenReturn(null);

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userRepository, keycloakUserService, userService);
        }
    }

    @Nested
    @DisplayName("User Synchronization Tests")
    class UserSynchronizationTests {

        @Test
        @DisplayName("Should not create user when user already exists in database")
        void shouldNotCreateUserWhenExists() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn(TEST_PATH);
            setupAuthenticatedJwtToken();
            when(userRepository.existsByKeycloakId(TEST_KEYCLOAK_ID)).thenReturn(true);

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(userRepository).existsByKeycloakId(TEST_KEYCLOAK_ID);
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(keycloakUserService, userService);
        }

        @Test
        @DisplayName("Should create user when user does not exist in database")
        void shouldCreateUserWhenNotExists() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn(TEST_PATH);
            setupAuthenticatedJwtToken();

            UserRepresentation userRep = createTestUserRepresentation();

            when(userRepository.existsByKeycloakId(TEST_KEYCLOAK_ID))
                .thenReturn(false)  // First check: user doesn't exist
                .thenReturn(true);  // Verification check: user was created
            when(keycloakUserService.getUserById(TEST_KEYCLOAK_ID, TEST_REALM))
                .thenReturn(Optional.of(userRep));

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(userRepository, times(2)).existsByKeycloakId(TEST_KEYCLOAK_ID);
            verify(keycloakUserService).getUserById(TEST_KEYCLOAK_ID, TEST_REALM);
            verify(userService).createUser(userRep);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle ResourceNotFoundException when user not found in Keycloak")
        void shouldHandleUserNotFoundInKeycloak() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn(TEST_PATH);
            setupAuthenticatedJwtToken();
            when(userRepository.existsByKeycloakId(TEST_KEYCLOAK_ID)).thenReturn(false);
            when(keycloakUserService.getUserById(TEST_KEYCLOAK_ID, TEST_REALM))
                .thenReturn(Optional.empty());

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(userRepository).existsByKeycloakId(TEST_KEYCLOAK_ID);
            verify(keycloakUserService).getUserById(TEST_KEYCLOAK_ID, TEST_REALM);
            verifyNoInteractions(userService);
            verify(filterChain).doFilter(request, response); // Should continue despite error
        }

        @Test
        @DisplayName("Should continue filter chain even when user creation fails")
        void shouldContinueChainWhenCreationFails() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn(TEST_PATH);
            setupAuthenticatedJwtToken();

            UserRepresentation userRep = createTestUserRepresentation();

            when(userRepository.existsByKeycloakId(TEST_KEYCLOAK_ID)).thenReturn(false);
            when(keycloakUserService.getUserById(TEST_KEYCLOAK_ID, TEST_REALM))
                .thenReturn(Optional.of(userRep));
            doThrow(new RuntimeException("Database error")).when(userService).createUser(any());

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response); // Should continue despite error
        }

        @Test
        @DisplayName("Should throw exception when user creation verification fails")
        void shouldThrowWhenVerificationFails() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn(TEST_PATH);
            setupAuthenticatedJwtToken();

            UserRepresentation userRep = createTestUserRepresentation();

            when(userRepository.existsByKeycloakId(TEST_KEYCLOAK_ID))
                .thenReturn(false)  // First check: user doesn't exist
                .thenReturn(false); // Verification check: user still doesn't exist (creation failed)
            when(keycloakUserService.getUserById(TEST_KEYCLOAK_ID, TEST_REALM))
                .thenReturn(Optional.of(userRep));

            // When
            userSyncFilter.doFilter(request, response, filterChain);

            // Then
            verify(userRepository, times(2)).existsByKeycloakId(TEST_KEYCLOAK_ID);
            verify(userService).createUser(userRep);
            verify(filterChain).doFilter(request, response); // Should still continue
        }
    }

    @Nested
    @DisplayName("EnsureUserExists Method Tests")
    class EnsureUserExistsTests {

        @Test
        @DisplayName("Should successfully create user when all conditions are met")
        void shouldCreateUserSuccessfully() {
            // Given
            UserRepresentation userRep = createTestUserRepresentation();

            when(userRepository.existsByKeycloakId(TEST_KEYCLOAK_ID))
                .thenReturn(false, true); // Not exists, then exists after creation
            when(keycloakUserService.getUserById(TEST_KEYCLOAK_ID, TEST_REALM))
                .thenReturn(Optional.of(userRep));

            // When
            assertDoesNotThrow(() -> userSyncFilter.ensureUserExists(TEST_KEYCLOAK_ID));

            // Then
            verify(userRepository, times(2)).existsByKeycloakId(TEST_KEYCLOAK_ID);
            verify(keycloakUserService).getUserById(TEST_KEYCLOAK_ID, TEST_REALM);
            verify(userService).createUser(userRep);
        }

        @Test
        @DisplayName("Should not attempt creation when user already exists")
        void shouldSkipCreationWhenUserExists() {
            // Given
            when(userRepository.existsByKeycloakId(TEST_KEYCLOAK_ID)).thenReturn(true);

            // When
            assertDoesNotThrow(() -> userSyncFilter.ensureUserExists(TEST_KEYCLOAK_ID));

            // Then
            verify(userRepository, times(1)).existsByKeycloakId(TEST_KEYCLOAK_ID);
            verifyNoInteractions(keycloakUserService, userService);
        }

        @Test
        @DisplayName("Should throw RuntimeException when user not found in Keycloak")
        void shouldThrowWhenUserNotInKeycloak() {
            // Given
            when(userRepository.existsByKeycloakId(TEST_KEYCLOAK_ID)).thenReturn(false);
            when(keycloakUserService.getUserById(TEST_KEYCLOAK_ID, TEST_REALM))
                .thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userSyncFilter.ensureUserExists(TEST_KEYCLOAK_ID));

            assertTrue(exception.getMessage().contains("User exists in JWT but not in Keycloak"));
            verify(userRepository).existsByKeycloakId(TEST_KEYCLOAK_ID);
            verify(keycloakUserService).getUserById(TEST_KEYCLOAK_ID, TEST_REALM);
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should throw RuntimeException when user creation verification fails")
        void shouldThrowWhenVerificationFails() {
            // Given
            UserRepresentation userRep = createTestUserRepresentation();

            when(userRepository.existsByKeycloakId(TEST_KEYCLOAK_ID))
                .thenReturn(false, false); // Not exists before and after creation
            when(keycloakUserService.getUserById(TEST_KEYCLOAK_ID, TEST_REALM))
                .thenReturn(Optional.of(userRep));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userSyncFilter.ensureUserExists(TEST_KEYCLOAK_ID));

            // The exception might be wrapped, so check the message or the cause
            assertTrue(exception.getMessage().contains("User was not properly created") ||
                       exception.getMessage().contains("Critical error during user synchronization"));
            verify(userRepository, times(2)).existsByKeycloakId(TEST_KEYCLOAK_ID);
            verify(userService).createUser(userRep);
        }
    }

    // Helper methods
    private void setupAuthenticatedJwtToken() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.isAuthenticated()).thenReturn(true);
        when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn(TEST_KEYCLOAK_ID);
    }

    private UserRepresentation createTestUserRepresentation() {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setId(TEST_KEYCLOAK_ID);
        userRep.setUsername(TEST_USERNAME);
        userRep.setEmail(TEST_USERNAME);
        userRep.setFirstName("Test");
        userRep.setLastName("User");
        userRep.setEnabled(true);
        return userRep;
    }
}