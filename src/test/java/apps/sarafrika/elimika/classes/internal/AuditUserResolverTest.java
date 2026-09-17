package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditUserResolverTest {

    @Mock
    private UserLookupService userLookupService;

    private AuditUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuditUserResolver(userLookupService);
    }

    @Test
    @DisplayName("created_by holds the Keycloak id, so that is matched first")
    void resolvesTheKeycloakId() {
        UUID userUuid = UUID.randomUUID();
        when(userLookupService.findUserUuidByKeycloakId("5f1c0a8e-keycloak")).thenReturn(Optional.of(userUuid));

        assertThat(resolver.userOf("5f1c0a8e-keycloak")).contains(userUuid);
        verify(userLookupService, never()).findUserUuidByEmail(any());
    }

    @Test
    @DisplayName("an older row that stored an email still resolves")
    void fallsBackToEmail() {
        UUID userUuid = UUID.randomUUID();
        when(userLookupService.findUserUuidByKeycloakId("manager@school.test")).thenReturn(Optional.empty());
        when(userLookupService.findUserUuidByEmail("manager@school.test")).thenReturn(Optional.of(userUuid));

        assertThat(resolver.userOf("manager@school.test")).contains(userUuid);
    }

    @Test
    @DisplayName("a blank audit value resolves to nobody without a lookup")
    void blankResolvesToNobody() {
        assertThat(resolver.userOf(null)).isEmpty();
        assertThat(resolver.userOf("  ")).isEmpty();
        verifyNoInteractions(userLookupService);
    }
}
