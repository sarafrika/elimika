package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.shared.event.timetabling.ClassSessionCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Class session completion drives obligation accrual")
class ClassSessionCompletedObligationListenerTest {

    @Mock
    private InstructorObligationService instructorObligationService;

    private ClassSessionCompletedObligationListener listener;

    @BeforeEach
    void setUp() {
        listener = new ClassSessionCompletedObligationListener(instructorObligationService);
    }

    @Test
    @DisplayName("a completed session is handed to the ledger with the session's own instructor")
    void completionAccruesTheObligation() {
        UUID sessionUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 3, 12, 30);
        when(instructorObligationService.accrueForCompletedSession(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        listener.handleClassSessionCompleted(new ClassSessionCompletedEvent(
                sessionUuid, classDefinitionUuid, instructorUuid, completedAt));

        verify(instructorObligationService)
                .accrueForCompletedSession(classDefinitionUuid, sessionUuid, instructorUuid, completedAt);
    }

    /**
     * The rethrow is load-bearing: Modulith's completion advisor marks a publication complete only on
     * a clean return, so swallowing this would turn a failed accrual into money quietly never owed.
     */
    @Test
    @DisplayName("a failed accrual is rethrown so its event publication stays incomplete")
    void failureIsRethrown() {
        when(instructorObligationService.accrueForCompletedSession(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> listener.handleClassSessionCompleted(new ClassSessionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now())))
                .isInstanceOf(InstructorObligationAccrualFailedException.class)
                .hasMessageContaining("will be retried");
    }

    @Test
    @DisplayName("an event with no session is ignored rather than failed")
    void anEmptyEventIsIgnored() {
        assertThatCode(() -> listener.handleClassSessionCompleted(
                new ClassSessionCompletedEvent(null, UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now())))
                .doesNotThrowAnyException();
        verify(instructorObligationService, never()).accrueForCompletedSession(any(), any(), any(), any());
    }
}
