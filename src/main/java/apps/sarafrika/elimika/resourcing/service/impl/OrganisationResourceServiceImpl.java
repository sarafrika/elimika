package apps.sarafrika.elimika.resourcing.service.impl;

import apps.sarafrika.elimika.resourcing.dto.OrganisationResourceDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceAvailabilityRuleDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceBookingDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceCalendarEntryDTO;
import apps.sarafrika.elimika.resourcing.factory.OrganisationResourceFactory;
import apps.sarafrika.elimika.resourcing.factory.ResourceAvailabilityRuleFactory;
import apps.sarafrika.elimika.resourcing.factory.ResourceBookingFactory;
import apps.sarafrika.elimika.resourcing.internal.AvailabilityRuleSupport;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrganisationResourceServiceImpl implements OrganisationResourceServiceInterface {

    private static final String RESOURCE_NOT_FOUND_TEMPLATE = "Organisation resource with UUID %s not found";
    private static final String RULE_NOT_FOUND_TEMPLATE = "Availability rule with UUID %s not found for resource %s";
    private static final List<ResourceBookingStatus> ACTIVE_STATUSES =
            List.of(ResourceBookingStatus.HOLD, ResourceBookingStatus.CONFIRMED);
    private static final int MAX_CALENDAR_DAYS = 400;

    private final OrganisationResourceRepository resourceRepository;
    private final ResourceAvailabilityRuleRepository ruleRepository;
    private final ResourceBookingRepository bookingRepository;
    private final UserLookupService userLookupService;
    private final DomainSecurityService domainSecurityService;

    // ===== Resources =====

    @Override
    public OrganisationResourceDTO createResource(UUID organisationUuid, OrganisationResourceDTO dto) {
        requireOrganisationManagerAccess(organisationUuid);
        validateResourceShape(dto.resourceType(), dto.seatCapacity(), dto.totalQuantity());
        if (resourceRepository.existsByOrganisationUuidAndNameIgnoreCase(organisationUuid, dto.name())) {
            throw new IllegalArgumentException(String.format(
                    "Organisation already has a resource named '%s'", dto.name()));
        }

        OrganisationResource entity = OrganisationResourceFactory.toEntity(dto);
        entity.setOrganisationUuid(organisationUuid);
        if (entity.getIsActive() == null) {
            entity.setIsActive(true);
        }
        OrganisationResource saved = resourceRepository.save(entity);
        log.info("Created {} resource '{}' ({}) for organisation {}",
                saved.getResourceType(), saved.getName(), saved.getUuid(), organisationUuid);
        return OrganisationResourceFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganisationResourceDTO> listResources(UUID organisationUuid,
                                                       ResourceType resourceType,
                                                       UUID branchUuid,
                                                       Boolean active,
                                                       Pageable pageable) {
        requireOrganisationManagerAccess(organisationUuid);
        Specification<OrganisationResource> spec =
                (root, query, cb) -> cb.equal(root.get("organisationUuid"), organisationUuid);
        if (resourceType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("resourceType"), resourceType));
        }
        if (branchUuid != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("branchUuid"), branchUuid));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), active));
        }
        return resourceRepository.findAll(spec, pageable).map(OrganisationResourceFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganisationResourceDTO getResource(UUID organisationUuid, UUID resourceUuid) {
        requireOrganisationManagerAccess(organisationUuid);
        return OrganisationResourceFactory.toDTO(requireOrganisationResource(organisationUuid, resourceUuid));
    }

    @Override
    public OrganisationResourceDTO updateResource(UUID organisationUuid, UUID resourceUuid, OrganisationResourceDTO dto) {
        requireOrganisationManagerAccess(organisationUuid);
        OrganisationResource entity = requireOrganisationResource(organisationUuid, resourceUuid);

        if (dto.resourceType() != null && dto.resourceType() != entity.getResourceType()) {
            throw new IllegalArgumentException("resource_type cannot be changed after a resource has been created");
        }
        if (dto.name() != null && !dto.name().equalsIgnoreCase(entity.getName())
                && resourceRepository.existsByOrganisationUuidAndNameIgnoreCase(organisationUuid, dto.name())) {
            throw new IllegalArgumentException(String.format(
                    "Organisation already has a resource named '%s'", dto.name()));
        }

        OrganisationResourceFactory.updateEntityFromDTO(entity, dto);
        validateResourceShape(entity.getResourceType(), entity.getSeatCapacity(), entity.getTotalQuantity());
        return OrganisationResourceFactory.toDTO(resourceRepository.save(entity));
    }

    @Override
    public void deactivateResource(UUID organisationUuid, UUID resourceUuid) {
        requireOrganisationManagerAccess(organisationUuid);
        OrganisationResource entity = requireOrganisationResource(organisationUuid, resourceUuid);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (bookingRepository.existsByResourceUuidAndStatusInAndEndTimeAfter(resourceUuid, ACTIVE_STATUSES, now)) {
            throw new IllegalStateException(String.format(
                    "Resource '%s' has future holds or confirmed bookings; release them before deactivating it.",
                    entity.getName()));
        }
        entity.setIsActive(false);
        resourceRepository.save(entity);
        log.info("Deactivated resource '{}' ({}) for organisation {}", entity.getName(), resourceUuid, organisationUuid);
    }

    // ===== Availability rules =====

    @Override
    public RuleChangeResult addAvailabilityRule(UUID organisationUuid, UUID resourceUuid, ResourceAvailabilityRuleDTO dto) {
        requireOrganisationManagerAccess(organisationUuid);
        requireOrganisationResource(organisationUuid, resourceUuid);
        validateRuleShape(dto);

        ResourceAvailabilityRule saved = ruleRepository.save(ResourceAvailabilityRuleFactory.toEntity(resourceUuid, dto));
        return new RuleChangeResult(
                ResourceAvailabilityRuleFactory.toDTO(saved),
                countActiveBookingsIntersecting(resourceUuid, saved));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceAvailabilityRuleDTO> listAvailabilityRules(UUID organisationUuid, UUID resourceUuid) {
        requireOrganisationManagerAccess(organisationUuid);
        requireOrganisationResource(organisationUuid, resourceUuid);
        return ResourceAvailabilityRuleFactory.toDTOList(
                ruleRepository.findByResourceUuidOrderByCreatedDateAsc(resourceUuid));
    }

    @Override
    public RuleChangeResult updateAvailabilityRule(UUID organisationUuid, UUID resourceUuid, UUID ruleUuid,
                                                   ResourceAvailabilityRuleDTO dto) {
        requireOrganisationManagerAccess(organisationUuid);
        requireOrganisationResource(organisationUuid, resourceUuid);
        ResourceAvailabilityRule rule = requireRule(resourceUuid, ruleUuid);
        validateRuleShape(dto);

        ResourceAvailabilityRuleFactory.updateEntityFromDTO(rule, dto);
        ResourceAvailabilityRule saved = ruleRepository.save(rule);
        return new RuleChangeResult(
                ResourceAvailabilityRuleFactory.toDTO(saved),
                countActiveBookingsIntersecting(resourceUuid, saved));
    }

    @Override
    public void deleteAvailabilityRule(UUID organisationUuid, UUID resourceUuid, UUID ruleUuid) {
        requireOrganisationManagerAccess(organisationUuid);
        requireOrganisationResource(organisationUuid, resourceUuid);
        ruleRepository.delete(requireRule(resourceUuid, ruleUuid));
    }

    // ===== Calendar & bookings =====

    @Override
    @Transactional(readOnly = true)
    public List<ResourceCalendarEntryDTO> getCalendar(UUID organisationUuid, UUID resourceUuid,
                                                      LocalDate startDate, LocalDate endDate) {
        requireOrganisationManagerAccess(organisationUuid);
        requireOrganisationResource(organisationUuid, resourceUuid);
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("A valid start_date and end_date (start_date <= end_date) are required");
        }
        if (startDate.plusDays(MAX_CALENDAR_DAYS).isBefore(endDate)) {
            throw new IllegalArgumentException(String.format(
                    "Calendar range cannot exceed %d days", MAX_CALENDAR_DAYS));
        }

        List<ResourceCalendarEntryDTO> entries = new ArrayList<>();
        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.plusDays(1).atStartOfDay();

        for (ResourceAvailabilityRule rule : ruleRepository.findByResourceUuidOrderByCreatedDateAsc(resourceUuid)) {
            if (rule.getSpecificStart() != null && rule.getSpecificEnd() != null) {
                if (rule.getSpecificStart().isBefore(rangeEnd) && rule.getSpecificEnd().isAfter(rangeStart)) {
                    entries.add(ruleEntry(rule, rule.getSpecificStart(), rule.getSpecificEnd()));
                }
                continue;
            }
            if (rule.getStartTime() == null || rule.getEndTime() == null) {
                continue;
            }
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                if (AvailabilityRuleSupport.ruleAppliesOn(rule, date)) {
                    entries.add(ruleEntry(rule, date.atTime(rule.getStartTime()), date.atTime(rule.getEndTime())));
                }
            }
        }

        List<ResourceBooking> bookings = bookingRepository
                .findByResourceUuidAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        resourceUuid, ACTIVE_STATUSES, rangeEnd, rangeStart);
        for (ResourceBooking booking : bookings) {
            entries.add(new ResourceCalendarEntryDTO(
                    booking.getStatus() == ResourceBookingStatus.HOLD ? "HOLD" : "CONFIRMED",
                    booking.getStartTime(),
                    booking.getEndTime(),
                    null,
                    booking.getUuid(),
                    booking.getJobUuid(),
                    booking.getClassDefinitionUuid(),
                    booking.getQuantity(),
                    null
            ));
        }

        entries.sort(Comparator.comparing(ResourceCalendarEntryDTO::startTime)
                .thenComparing(ResourceCalendarEntryDTO::entryType));
        return entries;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceBookingDTO> listBookings(UUID organisationUuid, UUID resourceUuid,
                                                 ResourceBookingStatus status,
                                                 LocalDate startDate, LocalDate endDate,
                                                 Pageable pageable) {
        requireOrganisationManagerAccess(organisationUuid);
        requireOrganisationResource(organisationUuid, resourceUuid);

        Specification<ResourceBooking> spec =
                (root, query, cb) -> cb.equal(root.get("resourceUuid"), resourceUuid);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (startDate != null) {
            LocalDateTime from = startDate.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("endTime"), from));
        }
        if (endDate != null) {
            LocalDateTime to = endDate.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("startTime"), to));
        }
        return bookingRepository.findAll(spec, pageable).map(ResourceBookingFactory::toDTO);
    }

    // ===== Helpers =====

    private ResourceCalendarEntryDTO ruleEntry(ResourceAvailabilityRule rule, LocalDateTime start, LocalDateTime end) {
        return new ResourceCalendarEntryDTO(
                rule.getRuleType() == AvailabilityRuleType.OPEN_HOURS ? "OPEN_HOURS" : "BLACKOUT",
                start, end, rule.getUuid(), null, null, null, null, rule.getNotes());
    }

    private void validateResourceShape(ResourceType resourceType, Integer seatCapacity, Integer totalQuantity) {
        if (resourceType == null) {
            throw new IllegalArgumentException("resource_type is required");
        }
        if (resourceType == ResourceType.VENUE) {
            if (seatCapacity == null || seatCapacity < 1) {
                throw new IllegalArgumentException("Venues require a seat_capacity of at least 1");
            }
            if (totalQuantity != null) {
                throw new IllegalArgumentException("Venues must not define total_quantity");
            }
        } else {
            if (totalQuantity == null || totalQuantity < 1) {
                throw new IllegalArgumentException("Equipment pools require a total_quantity of at least 1");
            }
            if (seatCapacity != null) {
                throw new IllegalArgumentException("Equipment pools must not define seat_capacity");
            }
        }
    }

    private void validateRuleShape(ResourceAvailabilityRuleDTO dto) {
        if (dto.ruleType() == null) {
            throw new IllegalArgumentException("rule_type is required");
        }
        boolean recurring = dto.startTime() != null || dto.endTime() != null;
        boolean specific = dto.specificStart() != null || dto.specificEnd() != null;

        if (recurring == specific) {
            throw new IllegalArgumentException(
                    "A rule must define either a recurring start_time/end_time window or a one-off specific_start/specific_end window, not both");
        }
        if (recurring) {
            if (dto.startTime() == null || dto.endTime() == null || !dto.startTime().isBefore(dto.endTime())) {
                throw new IllegalArgumentException("Recurring rules require start_time before end_time");
            }
            if (dto.effectiveStartDate() != null && dto.effectiveEndDate() != null
                    && dto.effectiveEndDate().isBefore(dto.effectiveStartDate())) {
                throw new IllegalArgumentException("effective_end_date cannot be before effective_start_date");
            }
        } else {
            if (dto.ruleType() != AvailabilityRuleType.BLACKOUT) {
                throw new IllegalArgumentException("One-off specific windows are only supported for BLACKOUT rules");
            }
            if (dto.specificStart() == null || dto.specificEnd() == null
                    || !dto.specificStart().isBefore(dto.specificEnd())) {
                throw new IllegalArgumentException("One-off rules require specific_start before specific_end");
            }
        }
    }

    /**
     * How many existing active bookings intersect a BLACKOUT rule (bookings are not
     * auto-invalidated by rule changes; the organisation resolves them manually).
     * OPEN_HOURS rules return 0 because another open window may still cover a booking.
     */
    private long countActiveBookingsIntersecting(UUID resourceUuid, ResourceAvailabilityRule rule) {
        if (rule.getRuleType() != AvailabilityRuleType.BLACKOUT) {
            return 0;
        }
        if (rule.getSpecificStart() != null && rule.getSpecificEnd() != null) {
            return bookingRepository.findByResourceUuidAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                    resourceUuid, ACTIVE_STATUSES, rule.getSpecificEnd(), rule.getSpecificStart()).size();
        }
        if (rule.getStartTime() == null || rule.getEndTime() == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime horizon = now.plusYears(5);
        return bookingRepository
                .findByResourceUuidAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        resourceUuid, ACTIVE_STATUSES, horizon, now)
                .stream()
                .filter(booking -> intersectsRecurringRule(rule, booking))
                .count();
    }

    private boolean intersectsRecurringRule(ResourceAvailabilityRule rule, ResourceBooking booking) {
        LocalDate date = booking.getStartTime().toLocalDate();
        LocalDate lastDate = booking.getEndTime().toLocalDate();
        while (!date.isAfter(lastDate)) {
            if (AvailabilityRuleSupport.ruleAppliesOn(rule, date)) {
                LocalDateTime ruleStart = date.atTime(rule.getStartTime());
                LocalDateTime ruleEnd = date.atTime(rule.getEndTime());
                if (ruleStart.isBefore(booking.getEndTime()) && ruleEnd.isAfter(booking.getStartTime())) {
                    return true;
                }
            }
            date = date.plusDays(1);
        }
        return false;
    }

    private OrganisationResource requireOrganisationResource(UUID organisationUuid, UUID resourceUuid) {
        OrganisationResource resource = resourceRepository.findByUuid(resourceUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(RESOURCE_NOT_FOUND_TEMPLATE, resourceUuid)));
        if (!resource.getOrganisationUuid().equals(organisationUuid)) {
            throw new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND_TEMPLATE, resourceUuid));
        }
        return resource;
    }

    private ResourceAvailabilityRule requireRule(UUID resourceUuid, UUID ruleUuid) {
        ResourceAvailabilityRule rule = ruleRepository.findByUuid(ruleUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(RULE_NOT_FOUND_TEMPLATE, ruleUuid, resourceUuid)));
        if (!rule.getResourceUuid().equals(resourceUuid)) {
            throw new ResourceNotFoundException(String.format(RULE_NOT_FOUND_TEMPLATE, ruleUuid, resourceUuid));
        }
        return rule;
    }

    private void requireOrganisationManagerAccess(UUID organisationUuid) {
        UUID currentUserUuid = domainSecurityService.getCurrentUserUuid();
        if (currentUserUuid == null) {
            throw new AccessDeniedException("An authenticated user is required for this action.");
        }

        boolean hasOrganisationUserAccess = userLookupService.userBelongsToOrganizationWithDomain(
                currentUserUuid, organisationUuid, UserDomain.organisation_user);
        boolean hasAdminAccess = userLookupService.userBelongsToOrganizationWithDomain(
                currentUserUuid, organisationUuid, UserDomain.admin);

        if (!hasOrganisationUserAccess && !hasAdminAccess) {
            throw new AccessDeniedException(String.format(
                    "User %s is not allowed to manage resources for organisation %s.",
                    currentUserUuid, organisationUuid));
        }
    }
}
