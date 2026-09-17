package apps.sarafrika.elimika.course.internal.training;

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
class TrainingSubmittersTest {

    @Mock
    private UserLookupService userLookupService;

    private TrainingSubmitters submitters;

    @BeforeEach
    void setUp() {
        submitters = new TrainingSubmitters(userLookupService);
    }

    @Test
    @DisplayName("created_by holds the Keycloak id, so that is matched first")
    void resolvesTheKeycloakId() {
        UUID userUuid = UUID.randomUUID();
        when(userLookupService.findUserUuidByKeycloakId("5f1c0a8e-keycloak")).thenReturn(Optional.of(userUuid));

        assertThat(submitters.userOf("5f1c0a8e-keycloak")).contains(userUuid);
        verify(userLookupService, never()).findUserUuidByEmail(any());
    }

    @Test
    @DisplayName("an older row that stored an email still resolves")
    void fallsBackToEmail() {
        UUID userUuid = UUID.randomUUID();
        when(userLookupService.findUserUuidByKeycloakId("manager@school.test")).thenReturn(Optional.empty());
        when(userLookupService.findUserUuidByEmail("manager@school.test")).thenReturn(Optional.of(userUuid));

        assertThat(submitters.userOf("manager@school.test")).contains(userUuid);
    }

    @Test
    @DisplayName("a blank audit value resolves to nobody without a lookup")
    void blankResolvesToNobody() {
        assertThat(submitters.userOf(null)).isEmpty();
        assertThat(submitters.userOf("  ")).isEmpty();
        verifyNoInteractions(userLookupService);
    }
}
