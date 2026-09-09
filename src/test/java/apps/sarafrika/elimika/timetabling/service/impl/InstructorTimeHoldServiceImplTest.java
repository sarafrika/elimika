package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.resourcing.spi.InstanceWindow;
import apps.sarafrika.elimika.shared.utils.recurrence.OccurrenceWindow;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.timetabling.model.InstructorTimeHold;
import apps.sarafrika.elimika.timetabling.repository.InstructorTimeHoldRepository;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldDTO;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldRequest;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldStatus;
import apps.sarafrika.elimika.timetabling.util.converter.InstructorTimeHoldStatusConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorTimeHoldServiceImplTest {

    private static final List<InstructorTimeHoldStatus> ACTIVE =
            List.of(InstructorTimeHoldStatus.TENTATIVE, InstructorTimeHoldStatus.FIRM);

    @Mock
    private InstructorTimeHoldRepository holdRepository;

    @Mock
    private OrganisationLookupService organisationLookupService;

    private InstructorTimeHoldServiceImpl holdService;

    private final UUID instructorUuid = UUID.randomUUID();
    private final UUID jobUuid = UUID.randomUUID();
    private final UUID applicationUuid = UUID.randomUUID();
    private final UUID organisationUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        holdService = new InstructorTimeHoldServiceImpl(holdRepository, organisationLookupService);
    }

    @Test
    void holdForApplicationPencilsInEveryWindowAsTentative() {
        holdService.holdForApplication(request(List.of(
                window(LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0)),
                window(LocalDateTime.of(2026, 5, 9, 9, 0), LocalDateTime.of(2026, 5, 9, 12, 0)))));

        ArgumentCaptor<List<InstructorTimeHold>> captor = ArgumentCaptor.forClass(List.class);
        verify(holdRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).allSatisfy(hold -> {
            assertThat(hold.getStatus()).isEqualTo(InstructorTimeHoldStatus.TENTATIVE);
            assertThat(hold.getInstructorUuid()).isEqualTo(instructorUuid);
            assertThat(hold.getJobUuid()).isEqualTo(jobUuid);
            assertThat(hold.getApplicationUuid()).isEqualTo(applicationUuid);
            assertThat(hold.getOrganisationUuid()).isEqualTo(organisationUuid);
            assertThat(hold.getTimezone()).isEqualTo("Africa/Nairobi");
        });
        assertThat(captor.getValue().getFirst().getStartTime())
                .isEqualTo(LocalDateTime.of(2026, 5, 2, 9, 0));
        assertThat(captor.getValue().getLast().getEndTime())
                .isEqualTo(LocalDateTime.of(2026, 5, 9, 12, 0));
    }

    @Test
    void holdForApplicationReplacesTheHoldsTheApplicationAlreadyHad() {
        InstructorTimeHold stale = hold(jobUuid, applicationUuid, InstructorTimeHoldStatus.TENTATIVE,
                LocalDateTime.of(2026, 4, 4, 9, 0), LocalDateTime.of(2026, 4, 4, 12, 0));
        when(holdRepository.findByApplicationUuidAndStatusIn(applicationUuid, ACTIVE))
                .thenReturn(List.of(stale));

        holdService.holdForApplication(request(List.of(
                window(LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0)))));

        // Re-applying must not leave the old windows sitting on the diary alongside the new ones.
        assertThat(stale.getStatus()).isEqualTo(InstructorTimeHoldStatus.RELEASED);
        assertThat(stale.getReleaseReason()).isEqualTo("Replaced by an updated hold");
        ArgumentCaptor<List<InstructorTimeHold>> captor = ArgumentCaptor.forClass(List.class);
        verify(holdRepository, times(2)).saveAll(captor.capture());
        assertThat(captor.getAllValues().getLast()).hasSize(1);
        assertThat(captor.getAllValues().getLast().getFirst().getStatus())
                .isEqualTo(InstructorTimeHoldStatus.TENTATIVE);
    }

    @Test
    void holdForApplicationWithoutWindowsTouchesNothing() {
        holdService.holdForApplication(request(List.of()));

        verifyNoInteractions(holdRepository);
    }

    @Test
    void holdForApplicationRejectsAWindowThatDoesNotMoveForwards() {
        InstructorTimeHoldRequest request = request(List.of(
                window(LocalDateTime.of(2026, 5, 2, 12, 0), LocalDateTime.of(2026, 5, 2, 9, 0))));

        assertThatThrownBy(() -> holdService.holdForApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start_time before end_time");
    }

    @Test
    void firmHoldsForApplicationPromotesOnlyTentativeOnes() {
        InstructorTimeHold tentative = hold(jobUuid, applicationUuid, InstructorTimeHoldStatus.TENTATIVE,
                LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0));
        when(holdRepository.findByApplicationUuidAndStatusIn(
                applicationUuid, List.of(InstructorTimeHoldStatus.TENTATIVE)))
                .thenReturn(List.of(tentative));

        holdService.firmHoldsForApplication(applicationUuid);

        assertThat(tentative.getStatus()).isEqualTo(InstructorTimeHoldStatus.FIRM);
        verify(holdRepository).saveAll(List.of(tentative));
    }

    @Test
    void firmOrCreateHoldsForApplicationWritesFirmHoldsWhenTheApplicationHeldNothing() {
        when(holdRepository.findByApplicationUuidAndStatusIn(applicationUuid, ACTIVE)).thenReturn(List.of());

        holdService.firmOrCreateHoldsForApplication(request(List.of(
                window(LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0)),
                window(LocalDateTime.of(2026, 5, 9, 9, 0), LocalDateTime.of(2026, 5, 9, 12, 0)))));

        ArgumentCaptor<List<InstructorTimeHold>> captor = ArgumentCaptor.forClass(List.class);
        verify(holdRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .allSatisfy(hold -> assertThat(hold.getStatus()).isEqualTo(InstructorTimeHoldStatus.FIRM));
    }

    @Test
    void firmOrCreateHoldsForApplicationPromotesExistingHoldsRatherThanDuplicatingThem() {
        InstructorTimeHold tentative = hold(jobUuid, applicationUuid, InstructorTimeHoldStatus.TENTATIVE,
                LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0));
        when(holdRepository.findByApplicationUuidAndStatusIn(applicationUuid, ACTIVE))
                .thenReturn(List.of(tentative));
        when(holdRepository.findByApplicationUuidAndStatusIn(
                applicationUuid, List.of(InstructorTimeHoldStatus.TENTATIVE)))
                .thenReturn(List.of(tentative));

        holdService.firmOrCreateHoldsForApplication(request(List.of(
                window(LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0)))));

        assertThat(tentative.getStatus()).isEqualTo(InstructorTimeHoldStatus.FIRM);
        verify(holdRepository, times(1)).saveAll(List.of(tentative));
    }

    @Test
    void releaseHoldsForApplicationStampsTheReasonAndTheMoment() {
        InstructorTimeHold firm = hold(jobUuid, applicationUuid, InstructorTimeHoldStatus.FIRM,
                LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0));
        when(holdRepository.findByApplicationUuidAndStatusIn(applicationUuid, ACTIVE))
                .thenReturn(List.of(firm));

        holdService.releaseHoldsForApplication(applicationUuid, "Application rejected");

        assertThat(firm.getStatus()).isEqualTo(InstructorTimeHoldStatus.RELEASED);
        assertThat(firm.getReleaseReason()).isEqualTo("Application rejected");
        assertThat(firm.getReleasedAt()).isNotNull();
        verify(holdRepository).saveAll(List.of(firm));
    }

    @Test
    void releaseHoldsForJobExceptKeepsTheHiredApplicationsHolds() {
        UUID hiredApplication = UUID.randomUUID();
        InstructorTimeHold hired = hold(jobUuid, hiredApplication, InstructorTimeHoldStatus.FIRM,
                LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0));
        InstructorTimeHold passedOver = hold(jobUuid, applicationUuid, InstructorTimeHoldStatus.TENTATIVE,
                LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0));
        when(holdRepository.findByJobUuidAndStatusIn(jobUuid, ACTIVE))
                .thenReturn(List.of(hired, passedOver));

        holdService.releaseHoldsForJobExcept(jobUuid, hiredApplication, "Another instructor was hired");

        assertThat(hired.getStatus()).isEqualTo(InstructorTimeHoldStatus.FIRM);
        assertThat(passedOver.getStatus()).isEqualTo(InstructorTimeHoldStatus.RELEASED);
        assertThat(passedOver.getReleaseReason()).isEqualTo("Another instructor was hired");
        verify(holdRepository).saveAll(List.of(passedOver));
    }

    @Test
    void confirmHoldsForJobConfirmsMatchingWindowsAndReleasesTheRest() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID scheduledInstanceUuid = UUID.randomUUID();
        InstructorTimeHold scheduled = hold(jobUuid, applicationUuid, InstructorTimeHoldStatus.FIRM,
                LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0));
        InstructorTimeHold rolledOver = hold(jobUuid, applicationUuid, InstructorTimeHoldStatus.FIRM,
                LocalDateTime.of(2026, 5, 9, 9, 0), LocalDateTime.of(2026, 5, 9, 12, 0));
        when(holdRepository.findByJobUuidAndStatusIn(jobUuid, ACTIVE))
                .thenReturn(List.of(scheduled, rolledOver));

        holdService.confirmHoldsForJob(jobUuid, classDefinitionUuid, List.of(new InstanceWindow(
                scheduledInstanceUuid,
                LocalDateTime.of(2026, 5, 2, 9, 0),
                LocalDateTime.of(2026, 5, 2, 12, 0))));

        assertThat(scheduled.getStatus()).isEqualTo(InstructorTimeHoldStatus.CONFIRMED);
        assertThat(scheduled.getClassDefinitionUuid()).isEqualTo(classDefinitionUuid);
        assertThat(scheduled.getScheduledInstanceUuid()).isEqualTo(scheduledInstanceUuid);
        // A window the class creation skipped is not a claim on anybody's diary any more.
        assertThat(rolledOver.getStatus()).isEqualTo(InstructorTimeHoldStatus.RELEASED);
        assertThat(rolledOver.getScheduledInstanceUuid()).isNull();
        verify(holdRepository).saveAll(List.of(scheduled, rolledOver));
    }

    @Test
    void findBlockingHoldsAsksOnlyForFirmHoldsAndSkipsTheJobsOwn() {
        UUID otherJobUuid = UUID.randomUUID();
        InstructorTimeHold ownHold = hold(jobUuid, applicationUuid, InstructorTimeHoldStatus.FIRM,
                LocalDateTime.of(2026, 5, 2, 9, 0), LocalDateTime.of(2026, 5, 2, 12, 0));
        InstructorTimeHold elsewhere = hold(otherJobUuid, UUID.randomUUID(), InstructorTimeHoldStatus.FIRM,
                LocalDateTime.of(2026, 5, 2, 10, 0), LocalDateTime.of(2026, 5, 2, 11, 0));
        when(holdRepository.findOverlappingHolds(eq(instructorUuid), anyCollection(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(ownHold, elsewhere));

        List<InstructorTimeHoldDTO> blocking = holdService.findBlockingHolds(
                instructorUuid,
                LocalDateTime.of(2026, 5, 2, 0, 0),
                LocalDateTime.of(2026, 5, 3, 0, 0),
                jobUuid);

        assertThat(blocking).singleElement()
                .satisfies(dto -> assertThat(dto.jobUuid()).isEqualTo(otherJobUuid));

        // TENTATIVE is never asked for: an instructor may apply to overlapping jobs.
        ArgumentCaptor<Collection<InstructorTimeHoldStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(holdRepository).findOverlappingHolds(eq(instructorUuid), statuses.capture(),
                any(LocalDateTime.class), any(LocalDateTime.class));
        assertThat(statuses.getValue()).containsExactly(InstructorTimeHoldStatus.FIRM);
    }

    @Test
    void findBlockingHoldsReturnsNothingForAWindowThatDoesNotMoveForwards() {
        LocalDateTime moment = LocalDateTime.of(2026, 5, 2, 9, 0);

        assertThat(holdService.findBlockingHolds(instructorUuid, moment, moment, jobUuid)).isEmpty();
        assertThat(holdService.findBlockingHolds(null, moment, moment.plusHours(1), jobUuid)).isEmpty();
        verifyNoInteractions(holdRepository);
    }

    @Test
    void findActiveHoldsCoversTheWholeEndDayAndNamesTheOrganisation() {
        InstructorTimeHold pencilled = hold(jobUuid, applicationUuid, InstructorTimeHoldStatus.TENTATIVE,
                LocalDateTime.of(2026, 5, 30, 9, 0), LocalDateTime.of(2026, 5, 30, 12, 0));
        when(holdRepository.findOverlappingHolds(eq(instructorUuid), eq(ACTIVE),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(pencilled));
        when(organisationLookupService.findOrganisationNames(anyCollection()))
                .thenReturn(Map.of(organisationUuid, "Sarafrika Technical College"));

        List<InstructorTimeHoldDTO> holds = holdService.findActiveHolds(
                instructorUuid, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 30));

        assertThat(holds).singleElement().satisfies(dto -> {
            assertThat(dto.status()).isEqualTo(InstructorTimeHoldStatus.TENTATIVE);
            assertThat(dto.organisationName()).isEqualTo("Sarafrika Technical College");
        });

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(holdRepository).findOverlappingHolds(eq(instructorUuid), eq(ACTIVE), from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
        // The end date is inclusive, so a session late on the last day still shows.
        assertThat(to.getValue()).isEqualTo(LocalDateTime.of(2026, 5, 31, 0, 0));
    }

    @Test
    void findActiveHoldsRejectsABackwardsRange() {
        assertThatThrownBy(() -> holdService.findActiveHolds(
                instructorUuid, LocalDate.of(2026, 5, 30), LocalDate.of(2026, 5, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    void statusConverterRoundTripsAndStillReadsLowerCaseRows() {
        InstructorTimeHoldStatusConverter converter = new InstructorTimeHoldStatusConverter();

        for (InstructorTimeHoldStatus status : InstructorTimeHoldStatus.values()) {
            String column = converter.convertToDatabaseColumn(status);
            assertThat(column).isEqualTo(status.name());
            assertThat(converter.convertToEntityAttribute(column)).isEqualTo(status);
            assertThat(converter.convertToEntityAttribute(column.toLowerCase())).isEqualTo(status);
        }

        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThatThrownBy(() -> converter.convertToEntityAttribute("pencilled"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private InstructorTimeHoldRequest request(List<OccurrenceWindow> windows) {
        return new InstructorTimeHoldRequest(
                instructorUuid,
                UUID.randomUUID(),
                organisationUuid,
                jobUuid,
                applicationUuid,
                "Weekend Data Analysis Bootcamp",
                "Africa/Nairobi",
                windows);
    }

    private OccurrenceWindow window(LocalDateTime start, LocalDateTime end) {
        return new OccurrenceWindow(start, end);
    }

    private InstructorTimeHold hold(UUID holdJobUuid,
                                    UUID holdApplicationUuid,
                                    InstructorTimeHoldStatus status,
                                    LocalDateTime start,
                                    LocalDateTime end) {
        InstructorTimeHold hold = new InstructorTimeHold();
        hold.setUuid(UUID.randomUUID());
        hold.setInstructorUuid(instructorUuid);
        hold.setOrganisationUuid(organisationUuid);
        hold.setJobUuid(holdJobUuid);
        hold.setApplicationUuid(holdApplicationUuid);
        hold.setTitle("Weekend Data Analysis Bootcamp");
        hold.setStartTime(start);
        hold.setEndTime(end);
        hold.setTimezone("Africa/Nairobi");
        hold.setStatus(status);
        return hold;
    }
}
