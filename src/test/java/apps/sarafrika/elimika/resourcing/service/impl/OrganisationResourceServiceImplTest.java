package apps.sarafrika.elimika.resourcing.service.impl;

import apps.sarafrika.elimika.resourcing.dto.OrganisationResourceDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceAvailabilityRuleDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceCalendarEntryDTO;
import apps.sarafrika.elimika.resourcing.model.OrganisationResource;
import apps.sarafrika.elimika.resourcing.model.ResourceAvailabilityRule;
import apps.sarafrika.elimika.resourcing.model.ResourceBooking;
import apps.sarafrika.elimika.resourcing.repository.OrganisationResourceRepository;
import apps.sarafrika.elimika.resourcing.repository.ResourceAvailabilityRuleRepository;
import apps.sarafrika.elimika.resourcing.repository.ResourceBookingRepository;
import apps.sarafrika.elimika.resourcing.service.OrganisationResourceServiceInterface;
import apps.sarafrika.elimika.resourcing.spi.AvailabilityRuleType;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationResourceServiceImplTest {

    private static final UUID ORG_UUID = UUID.randomUUID();
    private static final UUID USER_UUID = UUID.randomUUID();

    @Mock
    private OrganisationResourceRepository resourceRepository;
    @Mock
    private ResourceAvailabilityRuleRepository ruleRepository;
    @Mock
    private ResourceBookingRepository bookingRepository;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private DomainSecurityService domainSecurityService;

    private OrganisationResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrganisationResourceServiceImpl(
                resourceRepository, ruleRepository, bookingRepository, userLookupService, domainSecurityService);
        lenient().when(domainSecurityService.getCurrentUserUuid()).thenReturn(USER_UUID);
        lenient().when(userLookupService.userBelongsToOrganizationWithDomain(USER_UUID, ORG_UUID, UserDomain.organisation_user))
                .thenReturn(true);
    }

    // ===== create =====

    @Test
    void createVenueDefaultsToActive() {
        when(resourceRepository.existsByOrganisationUuidAndNameIgnoreCase(eq(ORG_UUID), any())).thenReturn(false);
        when(resourceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrganisationResourceDTO result = service.createResource(ORG_UUID, venueDto("Physics Lab", 30));

        assertThat(result.isActive()).isTrue();
        assertThat(result.organisationUuid()).isEqualTo(ORG_UUID);
        assertThat(result.resourceType()).isEqualTo(ResourceType.VENUE);
    }

    @Test
    void createVenueRequiresSeatCapacity() {
        assertThatThrownBy(() -> service.createResource(ORG_UUID, new OrganisationResourceDTO(
                null, null, null, ResourceType.VENUE, "Lab", null, null, null,
                null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seat_capacity");
    }

    @Test
    void createVenueRejectsTotalQuantity() {
        assertThatThrownBy(() -> service.createResource(ORG_UUID, new OrganisationResourceDTO(
                null, null, null, ResourceType.VENUE, "Lab", null, 20, 5,
                null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("total_quantity");
    }

    @Test
    void createEquipmentPoolRequiresTotalQuantity() {
        assertThatThrownBy(() -> service.createResource(ORG_UUID, new OrganisationResourceDTO(
                null, null, null, ResourceType.EQUIPMENT_POOL, "Laptops", null, null, null,
                null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("total_quantity");
    }

    @Test
    void createEquipmentPoolRejectsSeatCapacity() {
        assertThatThrownBy(() -> service.createResource(ORG_UUID, new OrganisationResourceDTO(
                null, null, null, ResourceType.EQUIPMENT_POOL, "Laptops", null, 10, 10,
                null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seat_capacity");
    }

    @Test
    void createRejectsDuplicateNamePerOrganisation() {
        when(resourceRepository.existsByOrganisationUuidAndNameIgnoreCase(ORG_UUID, "Physics Lab")).thenReturn(true);

        assertThatThrownBy(() -> service.createResource(ORG_UUID, venueDto("Physics Lab", 30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has a resource named");
    }

    @Test
    void createRequiresOrganisationManagerAccess() {
        when(userLookupService.userBelongsToOrganizationWithDomain(USER_UUID, ORG_UUID, UserDomain.organisation_user))
                .thenReturn(false);
        when(userLookupService.userBelongsToOrganizationWithDomain(USER_UUID, ORG_UUID, UserDomain.admin))
                .thenReturn(false);

        assertThatThrownBy(() -> service.createResource(ORG_UUID, venueDto("Physics Lab", 30)))
                .isInstanceOf(AccessDeniedException.class);
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void adminDomainAlsoGrantsAccess() {
        when(userLookupService.userBelongsToOrganizationWithDomain(USER_UUID, ORG_UUID, UserDomain.organisation_user))
                .thenReturn(false);
        when(userLookupService.userBelongsToOrganizationWithDomain(USER_UUID, ORG_UUID, UserDomain.admin))
                .thenReturn(true);
        when(resourceRepository.existsByOrganisationUuidAndNameIgnoreCase(eq(ORG_UUID), any())).thenReturn(false);
        when(resourceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.createResource(ORG_UUID, venueDto("Physics Lab", 30))).isNotNull();
    }

    // ===== update / deactivate =====

    @Test
    void updateRejectsResourceTypeChange() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        OrganisationResourceDTO change = new OrganisationResourceDTO(
                null, null, null, ResourceType.EQUIPMENT_POOL, "Physics Lab", null, null, 5,
                null, null, null, null, null, null);

        assertThatThrownBy(() -> service.updateResource(ORG_UUID, entity.getUuid(), change))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resource_type cannot be changed");
    }

    @Test
    void updateOfResourceFromAnotherOrganisationIsNotFound() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        entity.setOrganisationUuid(UUID.randomUUID());
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateResource(ORG_UUID, entity.getUuid(), venueDto("Physics Lab", 25)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivateRejectedWhileFutureActiveBookingsExist() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));
        when(bookingRepository.existsByResourceUuidAndStatusInAndEndTimeAfter(eq(entity.getUuid()), anyCollection(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.deactivateResource(ORG_UUID, entity.getUuid()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("future holds or confirmed bookings");
        assertThat(entity.getIsActive()).isTrue();
    }

    @Test
    void deactivateSucceedsWithoutFutureBookings() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));
        when(bookingRepository.existsByResourceUuidAndStatusInAndEndTimeAfter(eq(entity.getUuid()), anyCollection(), any()))
                .thenReturn(false);

        service.deactivateResource(ORG_UUID, entity.getUuid());

        assertThat(entity.getIsActive()).isFalse();
        verify(resourceRepository).save(entity);
    }

    // ===== availability rules =====

    @Test
    void addRuleRejectsWindowlessRule() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        ResourceAvailabilityRuleDTO rule = new ResourceAvailabilityRuleDTO(
                null, null, AvailabilityRuleType.OPEN_HOURS, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.addAvailabilityRule(ORG_UUID, entity.getUuid(), rule))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addRuleRejectsBothRecurringAndSpecificWindows() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        ResourceAvailabilityRuleDTO rule = new ResourceAvailabilityRuleDTO(
                null, null, AvailabilityRuleType.BLACKOUT, null,
                LocalTime.of(8, 0), LocalTime.of(10, 0),
                LocalDateTime.of(2026, 1, 5, 8, 0), LocalDateTime.of(2026, 1, 5, 10, 0),
                null, null, null);

        assertThatThrownBy(() -> service.addAvailabilityRule(ORG_UUID, entity.getUuid(), rule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not both");
    }

    @Test
    void addRuleRejectsSpecificWindowForOpenHours() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        ResourceAvailabilityRuleDTO rule = new ResourceAvailabilityRuleDTO(
                null, null, AvailabilityRuleType.OPEN_HOURS, null, null, null,
                LocalDateTime.of(2026, 1, 5, 8, 0), LocalDateTime.of(2026, 1, 5, 10, 0),
                null, null, null);

        assertThatThrownBy(() -> service.addAvailabilityRule(ORG_UUID, entity.getUuid(), rule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BLACKOUT");
    }

    @Test
    void addRuleRejectsInvertedRecurringWindow() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        ResourceAvailabilityRuleDTO rule = new ResourceAvailabilityRuleDTO(
                null, null, AvailabilityRuleType.OPEN_HOURS, null,
                LocalTime.of(18, 0), LocalTime.of(8, 0), null, null, null, null, null);

        assertThatThrownBy(() -> service.addAvailabilityRule(ORG_UUID, entity.getUuid(), rule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start_time before end_time");
    }

    @Test
    void addRuleRejectsInvertedEffectivePeriod() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        ResourceAvailabilityRuleDTO rule = new ResourceAvailabilityRuleDTO(
                null, null, AvailabilityRuleType.OPEN_HOURS, null,
                LocalTime.of(8, 0), LocalTime.of(18, 0), null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> service.addAvailabilityRule(ORG_UUID, entity.getUuid(), rule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effective_end_date");
    }

    @Test
    void addSpecificBlackoutReportsAffectedActiveBookings() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.findByResourceUuidAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(entity.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(new ResourceBooking(), new ResourceBooking()));

        ResourceAvailabilityRuleDTO rule = new ResourceAvailabilityRuleDTO(
                null, null, AvailabilityRuleType.BLACKOUT, null, null, null,
                LocalDateTime.of(2026, 3, 1, 0, 0), LocalDateTime.of(2026, 3, 2, 0, 0),
                null, null, "Maintenance");

        OrganisationResourceServiceInterface.RuleChangeResult result =
                service.addAvailabilityRule(ORG_UUID, entity.getUuid(), rule);

        assertThat(result.affectedActiveBookings()).isEqualTo(2);
        assertThat(result.rule().notes()).isEqualTo("Maintenance");
    }

    @Test
    void deleteRuleOfAnotherResourceIsNotFound() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));
        ResourceAvailabilityRule rule = new ResourceAvailabilityRule();
        rule.setUuid(UUID.randomUUID());
        rule.setResourceUuid(UUID.randomUUID());
        when(ruleRepository.findByUuid(rule.getUuid())).thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> service.deleteAvailabilityRule(ORG_UUID, entity.getUuid(), rule.getUuid()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== calendar =====

    @Test
    void calendarRejectsInvertedRange() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getCalendar(ORG_UUID, entity.getUuid(),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calendarRejectsExcessiveRange() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getCalendar(ORG_UUID, entity.getUuid(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    void calendarMergesExpandedRulesAndBookingsSortedByStart() {
        OrganisationResource entity = venueEntity("Physics Lab", 30);
        when(resourceRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        // Mondays-only open hours; 2026-01-05 is a Monday, range covers Mon-Tue
        ResourceAvailabilityRule openHours = new ResourceAvailabilityRule();
        openHours.setUuid(UUID.randomUUID());
        openHours.setResourceUuid(entity.getUuid());
        openHours.setRuleType(AvailabilityRuleType.OPEN_HOURS);
        openHours.setStartTime(LocalTime.of(8, 0));
        openHours.setEndTime(LocalTime.of(18, 0));
        openHours.setDaysOfWeek("MONDAY");

        ResourceAvailabilityRule oneOffBlackout = new ResourceAvailabilityRule();
        oneOffBlackout.setUuid(UUID.randomUUID());
        oneOffBlackout.setResourceUuid(entity.getUuid());
        oneOffBlackout.setRuleType(AvailabilityRuleType.BLACKOUT);
        oneOffBlackout.setSpecificStart(LocalDateTime.of(2026, 1, 6, 9, 0));
        oneOffBlackout.setSpecificEnd(LocalDateTime.of(2026, 1, 6, 11, 0));
        oneOffBlackout.setNotes("Maintenance");

        when(ruleRepository.findByResourceUuidOrderByCreatedDateAsc(entity.getUuid()))
                .thenReturn(List.of(openHours, oneOffBlackout));

        ResourceBooking hold = new ResourceBooking();
        hold.setUuid(UUID.randomUUID());
        hold.setResourceUuid(entity.getUuid());
        hold.setStatus(ResourceBookingStatus.HOLD);
        hold.setJobUuid(UUID.randomUUID());
        hold.setQuantity(1);
        hold.setStartTime(LocalDateTime.of(2026, 1, 5, 10, 0));
        hold.setEndTime(LocalDateTime.of(2026, 1, 5, 12, 0));
        when(bookingRepository.findByResourceUuidAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(entity.getUuid()), anyCollection(), any(), any()))
                .thenReturn(List.of(hold));

        List<ResourceCalendarEntryDTO> entries = service.getCalendar(ORG_UUID, entity.getUuid(),
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 6));

        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).entryType()).isEqualTo("OPEN_HOURS");
        assertThat(entries.get(0).startTime()).isEqualTo(LocalDateTime.of(2026, 1, 5, 8, 0));
        assertThat(entries.get(1).entryType()).isEqualTo("HOLD");
        assertThat(entries.get(1).jobUuid()).isEqualTo(hold.getJobUuid());
        assertThat(entries.get(2).entryType()).isEqualTo("BLACKOUT");
        assertThat(entries.get(2).notes()).isEqualTo("Maintenance");
    }

    // ===== helpers =====

    private OrganisationResourceDTO venueDto(String name, int seatCapacity) {
        return new OrganisationResourceDTO(
                null, null, null, ResourceType.VENUE, name, null, seatCapacity, null,
                null, null, null, null, null, null);
    }

    private OrganisationResource venueEntity(String name, int seatCapacity) {
        OrganisationResource entity = new OrganisationResource();
        entity.setUuid(UUID.randomUUID());
        entity.setOrganisationUuid(ORG_UUID);
        entity.setResourceType(ResourceType.VENUE);
        entity.setName(name);
        entity.setSeatCapacity(seatCapacity);
        entity.setIsActive(true);
        return entity;
    }
}
