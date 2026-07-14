package apps.sarafrika.elimika.resourcing.service.impl;

import apps.sarafrika.elimika.resourcing.model.OrganisationResource;
import apps.sarafrika.elimika.resourcing.model.ResourceAvailabilityRule;
import apps.sarafrika.elimika.resourcing.model.ResourceBooking;
import apps.sarafrika.elimika.resourcing.repository.OrganisationResourceRepository;
import apps.sarafrika.elimika.resourcing.repository.ResourceAvailabilityRuleRepository;
import apps.sarafrika.elimika.resourcing.repository.ResourceBookingRepository;
import apps.sarafrika.elimika.resourcing.spi.AvailabilityRuleType;
import apps.sarafrika.elimika.resourcing.spi.InstanceWindow;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingConflictException;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingRequest;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingSourceType;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import apps.sarafrika.elimika.resourcing.spi.ResourceConflictDetail;
import apps.sarafrika.elimika.resourcing.spi.ResourceConflictType;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.resourcing.spi.ResourceValidationReport;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.recurrence.OccurrenceWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceBookingServiceImplTest {

    private static final UUID ORG_UUID = UUID.randomUUID();
    private static final UUID JOB_UUID = UUID.randomUUID();
    // 2026-01-05 is a Monday
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 5, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 1, 5, 12, 0);

    @Mock
    private OrganisationResourceRepository resourceRepository;
    @Mock
    private ResourceAvailabilityRuleRepository ruleRepository;
    @Mock
    private ResourceBookingRepository bookingRepository;

    private ResourceBookingServiceImpl service;

    private OrganisationResource venue;
    private OrganisationResource equipmentPool;

    @BeforeEach
    void setUp() {
        service = new ResourceBookingServiceImpl(resourceRepository, ruleRepository, bookingRepository);
        venue = resource(ResourceType.VENUE, "Physics Lab", 30, null);
        equipmentPool = resource(ResourceType.EQUIPMENT_POOL, "Laptops", null, 10);
    }

    // ===== holdResourcesForJob =====

    @Test
    void holdResourcesForJobCreatesOneHoldPerWindow() {
        stubLockedResources(venue);
        stubNoRules(venue);
        stubNoOverlaps(venue);

        LocalDateTime secondStart = START.plusDays(7);
        service.holdResourcesForJob(JOB_UUID, ORG_UUID, List.of(request(venue, 1,
                new OccurrenceWindow(START, END),
                new OccurrenceWindow(secondStart, secondStart.plusHours(2)))));

        ArgumentCaptor<List<ResourceBooking>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository).saveAll(captor.capture());
        List<ResourceBooking> holds = captor.getValue();
        assertThat(holds).hasSize(2);
        assertThat(holds).allSatisfy(hold -> {
            assertThat(hold.getStatus()).isEqualTo(ResourceBookingStatus.HOLD);
            assertThat(hold.getSourceType()).isEqualTo(ResourceBookingSourceType.MARKETPLACE_JOB);
            assertThat(hold.getJobUuid()).isEqualTo(JOB_UUID);
            assertThat(hold.getOrganisationUuid()).isEqualTo(ORG_UUID);
            assertThat(hold.getQuantity()).isEqualTo(1);
        });
    }

    @Test
    void holdResourcesForJobWithNoRequestsIsNoOp() {
        service.holdResourcesForJob(JOB_UUID, ORG_UUID, List.of());

        verify(bookingRepository, never()).saveAll(anyList());
    }

    @Test
    void holdResourcesForJobRejectsMissingJobUuid() {
        assertThatThrownBy(() -> service.holdResourcesForJob(null, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void holdResourcesForJobThrowsWithFullReportOnConflict() {
        stubLockedResources(venue);
        stubNoRules(venue);
        ResourceBooking existing = booking(venue, ResourceBookingStatus.CONFIRMED, START.minusHours(1), END.plusHours(1), 1);
        when(bookingRepository.findActiveOverlaps(eq(venue.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOfSatisfying(ResourceBookingConflictException.class, ex -> {
                    ResourceValidationReport report = ex.getReport();
                    assertThat(report.clean()).isFalse();
                    assertThat(report.conflicts()).hasSize(1);
                    ResourceConflictDetail detail = report.conflicts().getFirst();
                    assertThat(detail.conflictType()).isEqualTo(ResourceConflictType.CONFIRMED_BOOKING);
                    assertThat(detail.conflictingBookingUuid()).isEqualTo(existing.getUuid());
                    assertThat(detail.requestedStart()).isEqualTo(START);
                });
        verify(bookingRepository, never()).saveAll(anyList());
    }

    @Test
    void holdResourcesForJobIgnoresItsOwnExistingHolds() {
        stubLockedResources(venue);
        stubNoRules(venue);
        ResourceBooking ownHold = booking(venue, ResourceBookingStatus.HOLD, START, END, 1);
        ownHold.setJobUuid(JOB_UUID);
        when(bookingRepository.findActiveOverlaps(eq(venue.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(ownHold));

        service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END))));

        verify(bookingRepository).saveAll(anyList());
    }

    @Test
    void holdResourcesForJobFlagsAnotherJobsHold() {
        stubLockedResources(venue);
        stubNoRules(venue);
        ResourceBooking otherHold = booking(venue, ResourceBookingStatus.HOLD, START, END, 1);
        otherHold.setJobUuid(UUID.randomUUID());
        when(bookingRepository.findActiveOverlaps(eq(venue.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(otherHold));

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOfSatisfying(ResourceBookingConflictException.class, ex ->
                        assertThat(ex.getReport().conflicts().getFirst().conflictType())
                                .isEqualTo(ResourceConflictType.ACTIVE_HOLD));
    }

    @Test
    void holdResourcesForJobRejectsResourceOfAnotherOrganisation() {
        OrganisationResource foreign = resource(ResourceType.VENUE, "Foreign Hall", 20, null);
        foreign.setOrganisationUuid(UUID.randomUUID());
        stubLockedResources(foreign);

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(foreign, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to organisation");
    }

    @Test
    void holdResourcesForJobRejectsUnknownResource() {
        when(resourceRepository.lockByUuids(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void holdResourcesForJobRejectsVenueQuantityAboveOne() {
        stubLockedResources(venue);

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 2, new OccurrenceWindow(START, END)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity 1");
    }

    // ===== equipment pool quantity aggregation =====

    @Test
    void equipmentPoolAllowsBookingThatExactlyExhaustsQuantity() {
        stubLockedResources(equipmentPool);
        stubNoRules(equipmentPool);
        ResourceBooking existing = booking(equipmentPool, ResourceBookingStatus.CONFIRMED, START, END, 6);
        when(bookingRepository.findActiveOverlaps(eq(equipmentPool.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(existing));

        service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(equipmentPool, 4, new OccurrenceWindow(START, END))));

        verify(bookingRepository).saveAll(anyList());
    }

    @Test
    void equipmentPoolRejectsBookingExceedingRemainingQuantity() {
        stubLockedResources(equipmentPool);
        stubNoRules(equipmentPool);
        ResourceBooking existing = booking(equipmentPool, ResourceBookingStatus.CONFIRMED, START, END, 6);
        when(bookingRepository.findActiveOverlaps(eq(equipmentPool.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(equipmentPool, 5, new OccurrenceWindow(START, END)))))
                .isInstanceOfSatisfying(ResourceBookingConflictException.class, ex -> {
                    ResourceConflictDetail detail = ex.getReport().conflicts().getFirst();
                    assertThat(detail.conflictType()).isEqualTo(ResourceConflictType.INSUFFICIENT_QUANTITY);
                    assertThat(detail.description()).contains("4 of 10");
                });
    }

    @Test
    void equipmentPoolAggregatesOverlappingHoldsAndConfirmedBookings() {
        stubLockedResources(equipmentPool);
        stubNoRules(equipmentPool);
        ResourceBooking confirmed = booking(equipmentPool, ResourceBookingStatus.CONFIRMED, START, END, 5);
        ResourceBooking otherHold = booking(equipmentPool, ResourceBookingStatus.HOLD, START, END, 3);
        otherHold.setJobUuid(UUID.randomUUID());
        when(bookingRepository.findActiveOverlaps(eq(equipmentPool.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(confirmed, otherHold));

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(equipmentPool, 3, new OccurrenceWindow(START, END)))))
                .isInstanceOf(ResourceBookingConflictException.class);
    }

    // ===== availability rules =====

    @Test
    void resourceWithoutOpenHoursRulesIsOpenAllTimes() {
        stubLockedResources(venue);
        stubNoRules(venue);
        stubNoOverlaps(venue);

        service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END))));

        verify(bookingRepository).saveAll(anyList());
    }

    @Test
    void windowExactlyFillingOpenHoursIsAccepted() {
        stubLockedResources(venue);
        stubRules(venue, openHours(venue, LocalTime.of(10, 0), LocalTime.of(12, 0), null));
        stubNoOverlaps(venue);

        service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END))));

        verify(bookingRepository).saveAll(anyList());
    }

    @Test
    void windowStartingBeforeOpenHoursIsRejected() {
        stubLockedResources(venue);
        stubRules(venue, openHours(venue, LocalTime.of(10, 30), LocalTime.of(18, 0), null));

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOfSatisfying(ResourceBookingConflictException.class, ex ->
                        assertThat(ex.getReport().conflicts().getFirst().conflictType())
                                .isEqualTo(ResourceConflictType.OUTSIDE_OPEN_HOURS));
    }

    @Test
    void openHoursRuleForOtherDaysDoesNotCoverWindow() {
        // START is a Monday; the only open window is on Tuesdays
        stubLockedResources(venue);
        ResourceAvailabilityRule tuesdayOnly = openHours(venue, LocalTime.of(8, 0), LocalTime.of(20, 0), "TUESDAY");
        stubRules(venue, tuesdayOnly);

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOf(ResourceBookingConflictException.class);
    }

    @Test
    void openHoursRuleOutsideEffectiveDatesDoesNotCoverWindow() {
        stubLockedResources(venue);
        ResourceAvailabilityRule expired = openHours(venue, LocalTime.of(8, 0), LocalTime.of(20, 0), null);
        expired.setEffectiveEndDate(LocalDate.of(2025, 12, 31));
        stubRules(venue, expired);

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOf(ResourceBookingConflictException.class);
    }

    @Test
    void multiDayWindowIsRejectedWhenOpenHoursExist() {
        stubLockedResources(venue);
        stubRules(venue, openHours(venue, LocalTime.of(0, 0), LocalTime.of(23, 59), null));

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END.plusDays(1))))))
                .isInstanceOf(ResourceBookingConflictException.class);
    }

    @Test
    void recurringBlackoutOnMatchingDayRejectsWindow() {
        stubLockedResources(venue);
        ResourceAvailabilityRule blackout = recurringBlackout(venue, LocalTime.of(11, 0), LocalTime.of(13, 0), "MONDAY");
        stubRules(venue, blackout);
        stubNoOverlaps(venue);

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOfSatisfying(ResourceBookingConflictException.class, ex ->
                        assertThat(ex.getReport().conflicts().getFirst().conflictType())
                                .isEqualTo(ResourceConflictType.BLACKOUT));
    }

    @Test
    void recurringBlackoutOnOtherDayDoesNotRejectWindow() {
        stubLockedResources(venue);
        stubRules(venue, recurringBlackout(venue, LocalTime.of(11, 0), LocalTime.of(13, 0), "FRIDAY"));
        stubNoOverlaps(venue);

        service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END))));

        verify(bookingRepository).saveAll(anyList());
    }

    @Test
    void blackoutTouchingWindowBoundaryDoesNotConflict() {
        stubLockedResources(venue);
        // blackout ends exactly when the window starts
        stubRules(venue, recurringBlackout(venue, LocalTime.of(8, 0), LocalTime.of(10, 0), null));
        stubNoOverlaps(venue);

        service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END))));

        verify(bookingRepository).saveAll(anyList());
    }

    @Test
    void oneOffBlackoutIntersectingWindowRejectsIt() {
        stubLockedResources(venue);
        ResourceAvailabilityRule oneOff = new ResourceAvailabilityRule();
        oneOff.setUuid(UUID.randomUUID());
        oneOff.setResourceUuid(venue.getUuid());
        oneOff.setRuleType(AvailabilityRuleType.BLACKOUT);
        oneOff.setSpecificStart(START.minusHours(1));
        oneOff.setSpecificEnd(START.plusMinutes(30));
        stubRules(venue, oneOff);
        stubNoOverlaps(venue);

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOf(ResourceBookingConflictException.class);
    }

    @Test
    void inactiveResourceIsReportedWithoutFurtherChecks() {
        venue.setIsActive(false);
        stubLockedResources(venue);
        stubNoRules(venue);

        assertThatThrownBy(() -> service.holdResourcesForJob(JOB_UUID, ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END)))))
                .isInstanceOfSatisfying(ResourceBookingConflictException.class, ex -> {
                    assertThat(ex.getReport().conflicts()).hasSize(1);
                    assertThat(ex.getReport().conflicts().getFirst().conflictType())
                            .isEqualTo(ResourceConflictType.RESOURCE_INACTIVE);
                });
    }

    // ===== confirm / release lifecycle =====

    @Test
    void confirmHoldsMatchesByExactWindowAndReleasesUnmatched() {
        UUID classUuid = UUID.randomUUID();
        UUID instanceUuid = UUID.randomUUID();
        ResourceBooking matched = booking(venue, ResourceBookingStatus.HOLD, START, END, 1);
        matched.setJobUuid(JOB_UUID);
        ResourceBooking unmatched = booking(venue, ResourceBookingStatus.HOLD, START.plusDays(7), END.plusDays(7), 1);
        unmatched.setJobUuid(JOB_UUID);
        when(bookingRepository.findByJobUuidAndStatus(JOB_UUID, ResourceBookingStatus.HOLD))
                .thenReturn(List.of(matched, unmatched));

        service.confirmHoldsForJob(JOB_UUID, classUuid, List.of(new InstanceWindow(instanceUuid, START, END)));

        assertThat(matched.getStatus()).isEqualTo(ResourceBookingStatus.CONFIRMED);
        assertThat(matched.getClassDefinitionUuid()).isEqualTo(classUuid);
        assertThat(matched.getScheduledInstanceUuid()).isEqualTo(instanceUuid);
        assertThat(unmatched.getStatus()).isEqualTo(ResourceBookingStatus.RELEASED);
        assertThat(unmatched.getReleasedAt()).isNotNull();
        verify(bookingRepository).saveAll(anyList());
    }

    @Test
    void confirmHoldsWithNoHoldsIsNoOp() {
        when(bookingRepository.findByJobUuidAndStatus(JOB_UUID, ResourceBookingStatus.HOLD))
                .thenReturn(List.of());

        service.confirmHoldsForJob(JOB_UUID, UUID.randomUUID(), List.of());

        verify(bookingRepository, never()).saveAll(anyList());
    }

    @Test
    void releaseHoldsForJobReleasesOnlyHolds() {
        ResourceBooking hold = booking(venue, ResourceBookingStatus.HOLD, START, END, 1);
        hold.setJobUuid(JOB_UUID);
        when(bookingRepository.findByJobUuidAndStatus(JOB_UUID, ResourceBookingStatus.HOLD))
                .thenReturn(List.of(hold));

        service.releaseHoldsForJob(JOB_UUID, "Job cancelled");

        assertThat(hold.getStatus()).isEqualTo(ResourceBookingStatus.RELEASED);
        assertThat(hold.getReleaseReason()).isEqualTo("Job cancelled");
        assertThat(hold.getReleasedAt()).isNotNull();
    }

    @Test
    void releaseHoldsForJobIsIdempotentWhenNothingHeld() {
        when(bookingRepository.findByJobUuidAndStatus(JOB_UUID, ResourceBookingStatus.HOLD))
                .thenReturn(List.of());

        service.releaseHoldsForJob(JOB_UUID, "Job cancelled");

        verify(bookingRepository, never()).saveAll(anyList());
    }

    // ===== instance lifecycle =====

    @Test
    void rescheduleInstanceBookingsMovesWindowsWhenClean() {
        UUID instanceUuid = UUID.randomUUID();
        ResourceBooking bookingRow = booking(venue, ResourceBookingStatus.CONFIRMED, START, END, 1);
        bookingRow.setScheduledInstanceUuid(instanceUuid);
        when(bookingRepository.findByScheduledInstanceUuidAndStatusIn(eq(instanceUuid), anyCollection()))
                .thenReturn(List.of(bookingRow));
        stubLockedResources(venue);
        stubNoRules(venue);
        stubNoOverlaps(venue);

        LocalDateTime newStart = START.plusDays(1);
        LocalDateTime newEnd = END.plusDays(1);
        service.rescheduleInstanceBookings(instanceUuid, newStart, newEnd);

        assertThat(bookingRow.getStartTime()).isEqualTo(newStart);
        assertThat(bookingRow.getEndTime()).isEqualTo(newEnd);
    }

    @Test
    void rescheduleInstanceBookingsExcludesItsOwnBookingsFromOverlapCheck() {
        UUID instanceUuid = UUID.randomUUID();
        ResourceBooking bookingRow = booking(venue, ResourceBookingStatus.CONFIRMED, START, END, 1);
        bookingRow.setScheduledInstanceUuid(instanceUuid);
        when(bookingRepository.findByScheduledInstanceUuidAndStatusIn(eq(instanceUuid), anyCollection()))
                .thenReturn(List.of(bookingRow));
        stubLockedResources(venue);
        stubNoRules(venue);
        // overlap query returns the booking itself at the (overlapping) new window
        when(bookingRepository.findActiveOverlaps(eq(venue.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(bookingRow));

        service.rescheduleInstanceBookings(instanceUuid, START.plusMinutes(30), END.plusMinutes(30));

        assertThat(bookingRow.getStartTime()).isEqualTo(START.plusMinutes(30));
    }

    @Test
    void rescheduleInstanceBookingsThrowsOnConflictAtNewWindow() {
        UUID instanceUuid = UUID.randomUUID();
        ResourceBooking bookingRow = booking(venue, ResourceBookingStatus.CONFIRMED, START, END, 1);
        bookingRow.setScheduledInstanceUuid(instanceUuid);
        ResourceBooking other = booking(venue, ResourceBookingStatus.CONFIRMED, START.plusDays(1), END.plusDays(1), 1);
        when(bookingRepository.findByScheduledInstanceUuidAndStatusIn(eq(instanceUuid), anyCollection()))
                .thenReturn(List.of(bookingRow));
        stubLockedResources(venue);
        stubNoRules(venue);
        when(bookingRepository.findActiveOverlaps(eq(venue.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(other));

        assertThatThrownBy(() -> service.rescheduleInstanceBookings(instanceUuid, START.plusDays(1), END.plusDays(1)))
                .isInstanceOf(ResourceBookingConflictException.class);
        assertThat(bookingRow.getStartTime()).isEqualTo(START);
    }

    @Test
    void rescheduleInstanceBookingsWithoutLinkedBookingsIsNoOp() {
        UUID instanceUuid = UUID.randomUUID();
        when(bookingRepository.findByScheduledInstanceUuidAndStatusIn(eq(instanceUuid), anyCollection()))
                .thenReturn(List.of());

        service.rescheduleInstanceBookings(instanceUuid, START, END);

        verify(resourceRepository, never()).lockByUuids(anyCollection());
    }

    @Test
    void releaseBookingsForInstanceCancelsActiveBookings() {
        UUID instanceUuid = UUID.randomUUID();
        ResourceBooking bookingRow = booking(venue, ResourceBookingStatus.CONFIRMED, START, END, 1);
        bookingRow.setScheduledInstanceUuid(instanceUuid);
        when(bookingRepository.findByScheduledInstanceUuidAndStatusIn(eq(instanceUuid), anyCollection()))
                .thenReturn(List.of(bookingRow));

        service.releaseBookingsForInstance(instanceUuid, "Session cancelled");

        assertThat(bookingRow.getStatus()).isEqualTo(ResourceBookingStatus.CANCELLED);
        assertThat(bookingRow.getReleaseReason()).isEqualTo("Session cancelled");
    }

    // ===== createConfirmedBookingsForInstance =====

    @Test
    void createConfirmedBookingsForInstanceBooksEveryResource() {
        UUID classUuid = UUID.randomUUID();
        UUID instanceUuid = UUID.randomUUID();
        stubLockedResources(venue, equipmentPool);
        when(ruleRepository.findByResourceUuidIn(anyCollection())).thenReturn(List.of());
        stubNoOverlaps(venue);
        stubNoOverlaps(equipmentPool);

        service.createConfirmedBookingsForInstance(classUuid, instanceUuid, START, END, List.of(
                new ResourceBookingRequest(venue.getUuid(), 1, List.of()),
                new ResourceBookingRequest(equipmentPool.getUuid(), 5, List.of())));

        ArgumentCaptor<List<ResourceBooking>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).allSatisfy(saved -> {
            assertThat(saved.getStatus()).isEqualTo(ResourceBookingStatus.CONFIRMED);
            assertThat(saved.getSourceType()).isEqualTo(ResourceBookingSourceType.CLASS_DEFINITION);
            assertThat(saved.getClassDefinitionUuid()).isEqualTo(classUuid);
            assertThat(saved.getScheduledInstanceUuid()).isEqualTo(instanceUuid);
        });
    }

    @Test
    void createConfirmedBookingsForInstanceExcludesOwnClassBookings() {
        UUID classUuid = UUID.randomUUID();
        stubLockedResources(venue);
        when(ruleRepository.findByResourceUuidIn(anyCollection())).thenReturn(List.of());
        ResourceBooking siblingBooking = booking(venue, ResourceBookingStatus.CONFIRMED, START, END, 1);
        siblingBooking.setClassDefinitionUuid(classUuid);
        when(bookingRepository.findActiveOverlaps(eq(venue.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(siblingBooking));

        service.createConfirmedBookingsForInstance(classUuid, UUID.randomUUID(), START, END,
                List.of(new ResourceBookingRequest(venue.getUuid(), 1, List.of())));

        verify(bookingRepository).saveAll(anyList());
    }

    // ===== validateBookings & lookups =====

    @Test
    void validateBookingsReturnsCleanReportWithoutPersistingAnything() {
        when(resourceRepository.findByUuidIn(anyCollection())).thenReturn(List.of(venue));
        stubNoRules(venue);
        stubNoOverlaps(venue);

        ResourceValidationReport report = service.validateBookings(ORG_UUID,
                List.of(request(venue, 1, new OccurrenceWindow(START, END))));

        assertThat(report.clean()).isTrue();
        verify(bookingRepository, never()).saveAll(anyList());
    }

    @Test
    void validateBookingsReportsEveryConflictingOccurrence() {
        when(resourceRepository.findByUuidIn(anyCollection())).thenReturn(List.of(venue));
        stubNoRules(venue);
        when(bookingRepository.findActiveOverlaps(eq(venue.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(booking(venue, ResourceBookingStatus.CONFIRMED, START.minusYears(1), END.plusYears(1), 1)));

        ResourceValidationReport report = service.validateBookings(ORG_UUID, List.of(request(venue, 1,
                new OccurrenceWindow(START, END),
                new OccurrenceWindow(START.plusDays(7), END.plusDays(7)))));

        assertThat(report.clean()).isFalse();
        assertThat(report.conflicts()).hasSize(2);
    }

    @Test
    void belongsToOrganisationChecksOwnership() {
        when(resourceRepository.findByUuid(venue.getUuid())).thenReturn(java.util.Optional.of(venue));

        assertThat(service.belongsToOrganisation(venue.getUuid(), ORG_UUID)).isTrue();
        assertThat(service.belongsToOrganisation(venue.getUuid(), UUID.randomUUID())).isFalse();
    }

    @Test
    void belongsToOrganisationIsFalseForUnknownResource() {
        when(resourceRepository.findByUuid(any())).thenReturn(java.util.Optional.empty());

        assertThat(service.belongsToOrganisation(UUID.randomUUID(), ORG_UUID)).isFalse();
    }

    @Test
    void findConflictsRequiresExistingResource() {
        when(resourceRepository.findByUuid(any())).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.findConflicts(UUID.randomUUID(), 1, START, END, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findConflictsRejectsInvertedWindow() {
        when(resourceRepository.findByUuid(venue.getUuid())).thenReturn(java.util.Optional.of(venue));
        when(ruleRepository.findByResourceUuidOrderByCreatedDateAsc(venue.getUuid())).thenReturn(List.of());

        assertThatThrownBy(() -> service.findConflicts(venue.getUuid(), 1, END, START, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== helpers =====

    private OrganisationResource resource(ResourceType type, String name, Integer seatCapacity, Integer totalQuantity) {
        OrganisationResource resource = new OrganisationResource();
        resource.setUuid(UUID.randomUUID());
        resource.setOrganisationUuid(ORG_UUID);
        resource.setResourceType(type);
        resource.setName(name);
        resource.setSeatCapacity(seatCapacity);
        resource.setTotalQuantity(totalQuantity);
        resource.setIsActive(true);
        return resource;
    }

    private ResourceBooking booking(OrganisationResource resource, ResourceBookingStatus status,
                                    LocalDateTime start, LocalDateTime end, int quantity) {
        ResourceBooking booking = new ResourceBooking();
        booking.setUuid(UUID.randomUUID());
        booking.setResourceUuid(resource.getUuid());
        booking.setOrganisationUuid(resource.getOrganisationUuid());
        booking.setStatus(status);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setQuantity(quantity);
        return booking;
    }

    private ResourceAvailabilityRule openHours(OrganisationResource resource, LocalTime start, LocalTime end, String days) {
        ResourceAvailabilityRule rule = new ResourceAvailabilityRule();
        rule.setUuid(UUID.randomUUID());
        rule.setResourceUuid(resource.getUuid());
        rule.setRuleType(AvailabilityRuleType.OPEN_HOURS);
        rule.setStartTime(start);
        rule.setEndTime(end);
        rule.setDaysOfWeek(days);
        return rule;
    }

    private ResourceAvailabilityRule recurringBlackout(OrganisationResource resource, LocalTime start, LocalTime end, String days) {
        ResourceAvailabilityRule rule = new ResourceAvailabilityRule();
        rule.setUuid(UUID.randomUUID());
        rule.setResourceUuid(resource.getUuid());
        rule.setRuleType(AvailabilityRuleType.BLACKOUT);
        rule.setStartTime(start);
        rule.setEndTime(end);
        rule.setDaysOfWeek(days);
        return rule;
    }

    private ResourceBookingRequest request(OrganisationResource resource, int quantity, OccurrenceWindow... windows) {
        return new ResourceBookingRequest(resource.getUuid(), quantity, List.of(windows));
    }

    private void stubLockedResources(OrganisationResource... resources) {
        lenient().when(resourceRepository.lockByUuids(anyCollection())).thenReturn(List.of(resources));
    }

    private void stubNoRules(OrganisationResource resource) {
        lenient().when(ruleRepository.findByResourceUuidIn(anyCollection())).thenReturn(List.of());
    }

    private void stubRules(OrganisationResource resource, ResourceAvailabilityRule... rules) {
        lenient().when(ruleRepository.findByResourceUuidIn(anyCollection())).thenReturn(List.of(rules));
    }

    private void stubNoOverlaps(OrganisationResource resource) {
        lenient().when(bookingRepository.findActiveOverlaps(eq(resource.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of());
    }
}
