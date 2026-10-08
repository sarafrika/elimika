package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the production filter chain and proves bearer calls, accepted or refused, never open an
 * HTTP session: no session on the request and no JSESSIONID cookie on the response.
 */
@WebMvcTest(controllers = SecurityConfigurationStatelessTest.ProbeController.class,
        properties = {"app.keycloak.realm=test-realm", "app.keycloak.admin.clientId=test-admin",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks"})
@Import({SecurityConfiguration.class, KeyCloakJwtAuthenticationConverter.class, UserSyncFilter.class,
        SecurityConfigurationStatelessTest.ProbeController.class, SecurityConfigurationStatelessTest.JwtStub.class})
@DisplayName("API filter chain is stateless")
class SecurityConfigurationStatelessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagementService userManagementService;

    @MockitoBean
    private DomainSecurityService domainSecurityService;

    @MockitoBean
    private RequestAuditService requestAuditService;

    @Test
    @DisplayName("an authenticated bearer call creates no session")
    void bearerCallCreatesNoSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/stateless-probe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getCookie("JSESSIONID")).isNull();
    }

    @Test
    @DisplayName("a refused anonymous call does not save the request in a session")
    void anonymousRefusalCreatesNoSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/stateless-probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @TestConfiguration
    static class JwtStub {

        @Bean
        JwtConfig jwtConfig() {
            return new JwtConfig() {
                @Override
                public JwtDecoder jwtDecoder() {
                    return token -> Jwt.withTokenValue(token)
                            .header("alg", "RS256")
                            .subject("kc-user")
                            .claim("azp", "elimika-web")
                            .issuedAt(Instant.now())
                            .expiresAt(Instant.now().plusSeconds(300))
                            .build();
                }
            };
        }
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/v1/stateless-probe")
        String probe() {
            return "ok";
        }
    }
}
