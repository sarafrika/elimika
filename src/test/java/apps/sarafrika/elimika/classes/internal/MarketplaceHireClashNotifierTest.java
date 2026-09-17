package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.dto.ClassSchedulingConflictDTO;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceHireClashNotifierTest {

    private static final String ORGANISATION_TYPE = "CLASS_MARKETPLACE_JOB_HIRE_BLOCKED_ORGANISATION";
    private static final String INSTRUCTOR_TYPE = "CLASS_MARKETPLACE_JOB_HIRE_BLOCKED_INSTRUCTOR";

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private InstructorLookupService instructorLookupService;

    @Mock
    private OrganisationLookupService organisationLookupService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MarketplaceHireClashNotifier notifier;

    private final UUID hiringUserUuid = UUID.randomUUID();
    private final UUID instructorUuid = UUID.randomUUID();
    private final UUID instructorUserUuid = UUID.randomUUID();
    private ClassMarketplaceJob job;

    @BeforeEach
    void setUp() {
        notifier = new MarketplaceHireClashNotifier(
                userLookupService, instructorLookupService, organisationLookupService, eventPublisher,
                new AuditUserResolver(userLookupService));
        job = new ClassMarketplaceJob();
        job.setUuid(UUID.randomUUID());
        job.setOrganisationUuid(UUID.randomUUID());
        job.setTitle("Grade 5 Piano");
        job.setCreatedBy("manager@school.test");
    }

    @Test
    void theEarliestClashLeadsAndEachReasonIsListedOnce() {
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(instructorUserUuid));
        when(organisationLookupService.findOrganisationName(job.getOrganisationUuid()))
                .thenReturn(Optional.of("Nairobi School"));

        notifier.notifyHireBlocked(job, UUID.randomUUID(), instructorUuid, hiringUserUuid, List.of(
                clash(LocalDateTime.of(2026, 6, 13, 14, 0), "Instructor is marked unavailable for this window"),
                clash(LocalDateTime.of(2026, 6, 6, 14, 0), "Instructor is marked unavailable for this window"),
                clash(LocalDateTime.of(2026, 6, 20, 14, 0), "Instructor is already committed to another class job in this window")));

        NotificationRequestedEvent instructorAlert = publishedInApp(INSTRUCTOR_TYPE);
        assertThat(instructorAlert.body())
                .contains("clashes with 3 of its sessions, the first on Jun 6, 2026 14:00 UTC.")
                .contains("What clashes: Instructor is marked unavailable for this window; "
                        + "Instructor is already committed to another class job in this window.");
        assertThat(instructorAlert.templateVariables())
                .containsEntry("first_clash_start", "2026-06-06T14:00")
                .containsEntry("first_clash_end", "2026-06-06T16:00")
                .containsEntry("organisation_name", "Nairobi School")
                .containsEntry("clash_reasons", List.of(
                        "Instructor is marked unavailable for this window",
                        "Instructor is already committed to another class job in this window"));
        assertThat(instructorAlert.dedupeKey())
                .isEqualTo("class-marketplace-job-hire-blocked:" + INSTRUCTOR_TYPE + ":" + job.getUuid() + ":"
                        + instructorUuid + ":2026-06-06T14:00:3");
    }

    @Test
    void aCreatorWhoAlsoPressedHireIsAlertedOnce() {
        // An older job stamped with an email still resolves through the fallback.
        when(userLookupService.findUserUuidByEmail("manager@school.test")).thenReturn(Optional.of(hiringUserUuid));

        notifier.notifyHireBlocked(job, UUID.randomUUID(), instructorUuid, hiringUserUuid,
                List.of(clash(LocalDateTime.of(2026, 6, 6, 14, 0), "Instructor is marked unavailable for this window")));

        // One organisation in-app alert; the instructor has no account here, so nothing reaches them.
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
        assertThat(publishedInApp(ORGANISATION_TYPE).recipientId()).isEqualTo(hiringUserUuid);
    }

    @Test
    void theJobCreatorIsFoundByTheKeycloakIdTheJobWasStampedWith() {
        UUID creatorUserUuid = UUID.randomUUID();
        job.setCreatedBy("5f1c0a8e-keycloak-subject");
        when(userLookupService.findUserUuidByKeycloakId("5f1c0a8e-keycloak-subject"))
                .thenReturn(Optional.of(creatorUserUuid));

        notifier.notifyHireBlocked(job, UUID.randomUUID(), instructorUuid, hiringUserUuid,
                List.of(clash(LocalDateTime.of(2026, 6, 6, 14, 0), "Instructor is marked unavailable for this window")));

        assertThat(publishedInAppRecipients(ORGANISATION_TYPE)).containsExactly(creatorUserUuid, hiringUserUuid);
        verify(userLookupService, never()).findUserUuidByEmail(any());
    }

    @Test
    void anOrganisationAlertThatFailsStillLetsTheInstructorHear() {
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(instructorUserUuid));
        lenient().doThrow(new IllegalStateException("registry down")).when(eventPublisher).publishEvent(
                argThat((Object event) -> event instanceof NotificationRequestedEvent request
                        && ORGANISATION_TYPE.equals(request.notificationType())));

        notifier.notifyHireBlocked(job, UUID.randomUUID(), instructorUuid, hiringUserUuid,
                List.of(clash(LocalDateTime.of(2026, 6, 6, 14, 0), "Instructor is marked unavailable for this window")));

        NotificationRequestedEvent instructorAlert = publishedInApp(INSTRUCTOR_TYPE);
        assertThat(instructorAlert.recipientId()).isEqualTo(instructorUserUuid);
        // An unnamed organisation still reads as a sentence at either end of the alert.
        assertThat(instructorAlert.body())
                .startsWith("The organisation tried to hire you for Grade 5 Piano")
                .endsWith("so the organisation can hire you.");
    }

    @Test
    void noClashesMeansNoAlerts() {
        notifier.notifyHireBlocked(job, UUID.randomUUID(), instructorUuid, hiringUserUuid, List.of());

        verifyNoInteractions(eventPublisher, userLookupService, instructorLookupService, organisationLookupService);
    }

    private NotificationRequestedEvent publishedInApp(String type) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(NotificationRequestedEvent.class::isInstance)
                .map(NotificationRequestedEvent.class::cast)
                .filter(event -> type.equals(event.notificationType()) && event.deliveryChannels().contains("in_app"))
                .reduce((first, second) -> first)
                .orElseThrow(() -> new AssertionError("No in-app " + type + " alert was published"));
    }

    private List<UUID> publishedInAppRecipients(String type) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(NotificationRequestedEvent.class::isInstance)
                .map(NotificationRequestedEvent.class::cast)
                .filter(event -> type.equals(event.notificationType()) && event.deliveryChannels().contains("in_app"))
                .map(NotificationRequestedEvent::recipientId)
                .toList();
    }

    private ClassSchedulingConflictDTO clash(LocalDateTime start, String reason) {
        return new ClassSchedulingConflictDTO(start, start.plusHours(2), List.of(reason));
    }
}
