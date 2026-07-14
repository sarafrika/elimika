package apps.sarafrika.elimika.resourcing.service.impl;

import apps.sarafrika.elimika.resourcing.factory.OrganisationResourceFactory;
import apps.sarafrika.elimika.resourcing.internal.AvailabilityRuleSupport;
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
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingService;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingSourceType;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import apps.sarafrika.elimika.resourcing.spi.ResourceConflictDetail;
import apps.sarafrika.elimika.resourcing.spi.ResourceConflictType;
import apps.sarafrika.elimika.resourcing.spi.ResourceLookupService;
import apps.sarafrika.elimika.resourcing.spi.ResourceSummary;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.resourcing.spi.ResourceValidationReport;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.recurrence.OccurrenceWindow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResourceBookingServiceImpl implements ResourceBookingService, ResourceLookupService {

    private static final List<ResourceBookingStatus> ACTIVE_STATUSES =
            List.of(ResourceBookingStatus.HOLD, ResourceBookingStatus.CONFIRMED);
    private static final String RESOURCE_NOT_FOUND_TEMPLATE = "Organisation resource with UUID %s not found";

    private final OrganisationResourceRepository resourceRepository;
    private final ResourceAvailabilityRuleRepository ruleRepository;
    private final ResourceBookingRepository bookingRepository;

    // ===== ResourceBookingService =====

    @Override
    @Transactional(readOnly = true)
    public ResourceValidationReport validateBookings(UUID organisationUuid, List<ResourceBookingRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ResourceValidationReport.empty();
        }
        Map<UUID, OrganisationResource> resources = loadAndVerifyResources(organisationUuid, requests, false);
        List<ResourceConflictDetail> conflicts = collectConflicts(resources, requests, Exclusions.none());
        return ResourceValidationReport.withConflicts(conflicts);
    }

    @Override
    public void holdResourcesForJob(UUID jobUuid, UUID organisationUuid, List<ResourceBookingRequest> requests) {
        if (jobUuid == null) {
            throw new IllegalArgumentException("Job UUID is required to hold resources");
        }
        if (requests == null || requests.isEmpty()) {
            return;
        }

        Map<UUID, OrganisationResource> resources = loadAndVerifyResources(organisationUuid, requests, true);
        List<ResourceConflictDetail> conflicts = collectConflicts(resources, requests, Exclusions.forJob(jobUuid));
        if (!conflicts.isEmpty()) {
            throw new ResourceBookingConflictException(
                    "Requested resources are not available for the job's session schedule",
                    ResourceValidationReport.withConflicts(conflicts));
        }

        List<ResourceBooking> holds = new ArrayList<>();
        for (ResourceBookingRequest request : requests) {
            OrganisationResource resource = resources.get(request.resourceUuid());
            for (OccurrenceWindow window : request.windows()) {
                ResourceBooking hold = new ResourceBooking();
                hold.setResourceUuid(resource.getUuid());
                hold.setOrganisationUuid(resource.getOrganisationUuid());
                hold.setStatus(ResourceBookingStatus.HOLD);
                hold.setQuantity(request.quantity());
                hold.setStartTime(window.start());
                hold.setEndTime(window.end());
                hold.setSourceType(ResourceBookingSourceType.MARKETPLACE_JOB);
                hold.setJobUuid(jobUuid);
                holds.add(hold);
            }
        }
        bookingRepository.saveAll(holds);
        log.info("Placed {} resource holds for marketplace job {}", holds.size(), jobUuid);
    }

    @Override
    public void releaseHoldsForJob(UUID jobUuid, String reason) {
        List<ResourceBooking> holds = bookingRepository.findByJobUuidAndStatus(jobUuid, ResourceBookingStatus.HOLD);
        if (holds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (ResourceBooking hold : holds) {
            hold.setStatus(ResourceBookingStatus.RELEASED);
            hold.setReleasedAt(now);
            hold.setReleaseReason(reason);
        }
        bookingRepository.saveAll(holds);
        log.info("Released {} resource holds for marketplace job {}: {}", holds.size(), jobUuid, reason);
    }

    @Override
    public void confirmHoldsForJob(UUID jobUuid, UUID classDefinitionUuid, List<InstanceWindow> instanceWindows) {
        List<ResourceBooking> holds = bookingRepository.findByJobUuidAndStatus(jobUuid, ResourceBookingStatus.HOLD);
        if (holds.isEmpty()) {
            return;
        }

        List<InstanceWindow> windows = instanceWindows == null ? List.of() : instanceWindows;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int confirmed = 0;
        for (ResourceBooking hold : holds) {
            Optional<InstanceWindow> match = windows.stream()
                    .filter(w -> hold.getStartTime().equals(w.startTime()) && hold.getEndTime().equals(w.endTime()))
                    .findFirst();
            if (match.isPresent()) {
                hold.setStatus(ResourceBookingStatus.CONFIRMED);
                hold.setClassDefinitionUuid(classDefinitionUuid);
                hold.setScheduledInstanceUuid(match.get().scheduledInstanceUuid());
                confirmed++;
            } else {
                hold.setStatus(ResourceBookingStatus.RELEASED);
                hold.setReleasedAt(now);
                hold.setReleaseReason("Occurrence was not scheduled when the class was created");
            }
        }
        bookingRepository.saveAll(holds);
        log.info("Confirmed {} of {} resource holds for marketplace job {} onto class {}",
                confirmed, holds.size(), jobUuid, classDefinitionUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceConflictDetail> findConflicts(UUID resourceUuid,
                                                      int quantity,
                                                      LocalDateTime start,
                                                      LocalDateTime end,
                                                      UUID excludeJobUuid,
                                                      UUID excludeClassDefinitionUuid) {
        OrganisationResource resource = requireResource(resourceUuid);
        List<ResourceAvailabilityRule> rules = ruleRepository.findByResourceUuidOrderByCreatedDateAsc(resourceUuid);
        return conflictsForWindow(resource, rules, quantity, start, end,
                new Exclusions(excludeJobUuid, excludeClassDefinitionUuid, null));
    }

    @Override
    public void createConfirmedBookingsForInstance(UUID classDefinitionUuid,
                                                   UUID scheduledInstanceUuid,
                                                   LocalDateTime start,
                                                   LocalDateTime end,
                                                   List<ResourceBookingRequest> resources) {
        if (resources == null || resources.isEmpty()) {
            return;
        }
        List<ResourceBookingRequest> singleWindowRequests = resources.stream()
                .map(r -> new ResourceBookingRequest(r.resourceUuid(), r.quantity(), List.of(new OccurrenceWindow(start, end))))
                .toList();

        Map<UUID, OrganisationResource> loaded = loadAndVerifyResources(null, singleWindowRequests, true);
        List<ResourceConflictDetail> conflicts =
                collectConflicts(loaded, singleWindowRequests, Exclusions.forClassDefinition(classDefinitionUuid));
        if (!conflicts.isEmpty()) {
            throw new ResourceBookingConflictException(
                    "Requested resources are not available for the new session window",
                    ResourceValidationReport.withConflicts(conflicts));
        }

        List<ResourceBooking> bookings = new ArrayList<>();
        for (ResourceBookingRequest request : singleWindowRequests) {
            OrganisationResource resource = loaded.get(request.resourceUuid());
            ResourceBooking booking = new ResourceBooking();
            booking.setResourceUuid(resource.getUuid());
            booking.setOrganisationUuid(resource.getOrganisationUuid());
            booking.setStatus(ResourceBookingStatus.CONFIRMED);
            booking.setQuantity(request.quantity());
            booking.setStartTime(start);
            booking.setEndTime(end);
            booking.setSourceType(ResourceBookingSourceType.CLASS_DEFINITION);
            booking.setClassDefinitionUuid(classDefinitionUuid);
            booking.setScheduledInstanceUuid(scheduledInstanceUuid);
            bookings.add(booking);
        }
        bookingRepository.saveAll(bookings);
    }

    @Override
    public void rescheduleInstanceBookings(UUID scheduledInstanceUuid, LocalDateTime newStart, LocalDateTime newEnd) {
        List<ResourceBooking> bookings =
                bookingRepository.findByScheduledInstanceUuidAndStatusIn(scheduledInstanceUuid, ACTIVE_STATUSES);
        if (bookings.isEmpty()) {
            return;
        }

        Set<UUID> resourceUuids = bookings.stream().map(ResourceBooking::getResourceUuid)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, OrganisationResource> resources = lockResources(resourceUuids);
        Map<UUID, List<ResourceAvailabilityRule>> rulesByResource = loadRules(resourceUuids);

        List<ResourceConflictDetail> conflicts = new ArrayList<>();
        for (ResourceBooking booking : bookings) {
            OrganisationResource resource = resources.get(booking.getResourceUuid());
            conflicts.addAll(conflictsForWindow(
                    resource,
                    rulesByResource.getOrDefault(resource.getUuid(), List.of()),
                    booking.getQuantity(),
                    newStart,
                    newEnd,
                    new Exclusions(booking.getJobUuid(), booking.getClassDefinitionUuid(), scheduledInstanceUuid)));
        }
        if (!conflicts.isEmpty()) {
            throw new ResourceBookingConflictException(
                    "Linked resources are not available for the new session window",
                    ResourceValidationReport.withConflicts(conflicts));
        }

        for (ResourceBooking booking : bookings) {
            booking.setStartTime(newStart);
            booking.setEndTime(newEnd);
        }
        bookingRepository.saveAll(bookings);
    }

    @Override
    public void releaseBookingsForInstance(UUID scheduledInstanceUuid, String reason) {
        List<ResourceBooking> bookings =
                bookingRepository.findByScheduledInstanceUuidAndStatusIn(scheduledInstanceUuid, ACTIVE_STATUSES);
        if (bookings.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (ResourceBooking booking : bookings) {
            booking.setStatus(ResourceBookingStatus.CANCELLED);
            booking.setReleasedAt(now);
            booking.setReleaseReason(reason);
        }
        bookingRepository.saveAll(bookings);
    }

    // ===== ResourceLookupService =====

    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceSummary> getResource(UUID resourceUuid) {
        return resourceRepository.findByUuid(resourceUuid).map(OrganisationResourceFactory::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean belongsToOrganisation(UUID resourceUuid, UUID organisationUuid) {
        return resourceRepository.findByUuid(resourceUuid)
                .map(resource -> resource.getOrganisationUuid().equals(organisationUuid))
                .orElse(false);
    }

    // ===== Conflict engine =====

    /**
     * Bookings excluded from overlap checks so a reservation never conflicts with
     * itself: a job's own holds, a class definition's own bookings, or the bookings
     * of the scheduled instance currently being moved.
     */
    private record Exclusions(UUID jobUuid, UUID classDefinitionUuid, UUID scheduledInstanceUuid) {

        static Exclusions none() {
            return new Exclusions(null, null, null);
        }

        static Exclusions forJob(UUID jobUuid) {
            return new Exclusions(jobUuid, null, null);
        }

        static Exclusions forClassDefinition(UUID classDefinitionUuid) {
            return new Exclusions(null, classDefinitionUuid, null);
        }

        boolean excludes(ResourceBooking booking) {
            if (jobUuid != null && jobUuid.equals(booking.getJobUuid())) {
                return true;
            }
            if (classDefinitionUuid != null && classDefinitionUuid.equals(booking.getClassDefinitionUuid())) {
                return true;
            }
            return scheduledInstanceUuid != null && scheduledInstanceUuid.equals(booking.getScheduledInstanceUuid());
        }
    }

    private List<ResourceConflictDetail> collectConflicts(Map<UUID, OrganisationResource> resources,
                                                          List<ResourceBookingRequest> requests,
                                                          Exclusions exclusions) {
        Map<UUID, List<ResourceAvailabilityRule>> rulesByResource = loadRules(resources.keySet());
        List<ResourceConflictDetail> conflicts = new ArrayList<>();
        for (ResourceBookingRequest request : requests) {
            OrganisationResource resource = resources.get(request.resourceUuid());
            List<ResourceAvailabilityRule> rules = rulesByResource.getOrDefault(resource.getUuid(), List.of());
            for (OccurrenceWindow window : request.windows()) {
                conflicts.addAll(conflictsForWindow(
                        resource, rules, request.quantity(), window.start(), window.end(), exclusions));
            }
        }
        return conflicts;
    }

    private List<ResourceConflictDetail> conflictsForWindow(OrganisationResource resource,
                                                            List<ResourceAvailabilityRule> rules,
                                                            int quantity,
                                                            LocalDateTime start,
                                                            LocalDateTime end,
                                                            Exclusions exclusions) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Booking windows require start_time before end_time");
        }

        List<ResourceConflictDetail> conflicts = new ArrayList<>();

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            conflicts.add(conflict(resource, start, end, ResourceConflictType.RESOURCE_INACTIVE, null, null,
                    String.format("Resource '%s' is deactivated", resource.getName())));
            return conflicts;
        }

        checkOpenHours(resource, rules, start, end, conflicts);
        checkBlackouts(resource, rules, start, end, conflicts);
        checkBookingOverlaps(resource, quantity, start, end, exclusions, conflicts);

        return conflicts;
    }

    private void checkOpenHours(OrganisationResource resource,
                                List<ResourceAvailabilityRule> rules,
                                LocalDateTime start,
                                LocalDateTime end,
                                List<ResourceConflictDetail> conflicts) {
        List<ResourceAvailabilityRule> openRules = rules.stream()
                .filter(rule -> rule.getRuleType() == AvailabilityRuleType.OPEN_HOURS)
                .toList();
        if (openRules.isEmpty()) {
            return;
        }

        boolean fits = false;
        if (start.toLocalDate().equals(end.toLocalDate())) {
            LocalDate date = start.toLocalDate();
            fits = openRules.stream().anyMatch(rule -> ruleAppliesOn(rule, date)
                    && rule.getStartTime() != null && rule.getEndTime() != null
                    && !start.toLocalTime().isBefore(rule.getStartTime())
                    && !end.toLocalTime().isAfter(rule.getEndTime()));
        }
        if (!fits) {
            conflicts.add(conflict(resource, start, end, ResourceConflictType.OUTSIDE_OPEN_HOURS, null, null,
                    String.format("Window falls outside the open hours of '%s'", resource.getName())));
        }
    }

    private void checkBlackouts(OrganisationResource resource,
                                List<ResourceAvailabilityRule> rules,
                                LocalDateTime start,
                                LocalDateTime end,
                                List<ResourceConflictDetail> conflicts) {
        for (ResourceAvailabilityRule rule : rules) {
            if (rule.getRuleType() != AvailabilityRuleType.BLACKOUT) {
                continue;
            }
            if (rule.getSpecificStart() != null && rule.getSpecificEnd() != null) {
                if (rule.getSpecificStart().isBefore(end) && rule.getSpecificEnd().isAfter(start)) {
                    conflicts.add(conflict(resource, start, end, ResourceConflictType.BLACKOUT, null, null,
                            blackoutDescription(resource, rule)));
                }
                continue;
            }
            if (rule.getStartTime() == null || rule.getEndTime() == null) {
                continue;
            }
            LocalDate date = start.toLocalDate();
            LocalDate lastDate = end.toLocalDate();
            while (!date.isAfter(lastDate)) {
                if (ruleAppliesOn(rule, date)) {
                    LocalDateTime blackoutStart = date.atTime(rule.getStartTime());
                    LocalDateTime blackoutEnd = date.atTime(rule.getEndTime());
                    if (blackoutStart.isBefore(end) && blackoutEnd.isAfter(start)) {
                        conflicts.add(conflict(resource, start, end, ResourceConflictType.BLACKOUT, null, null,
                                blackoutDescription(resource, rule)));
                        break;
                    }
                }
                date = date.plusDays(1);
            }
        }
    }

    private void checkBookingOverlaps(OrganisationResource resource,
                                      int quantity,
                                      LocalDateTime start,
                                      LocalDateTime end,
                                      Exclusions exclusions,
                                      List<ResourceConflictDetail> conflicts) {
        List<ResourceBooking> overlaps = bookingRepository
                .findActiveOverlaps(resource.getUuid(), ACTIVE_STATUSES, start, end)
                .stream()
                .filter(booking -> !exclusions.excludes(booking))
                .toList();
        if (overlaps.isEmpty()) {
            return;
        }

        if (resource.getResourceType() == ResourceType.VENUE) {
            for (ResourceBooking overlap : overlaps) {
                ResourceConflictType type = overlap.getStatus() == ResourceBookingStatus.HOLD
                        ? ResourceConflictType.ACTIVE_HOLD
                        : ResourceConflictType.CONFIRMED_BOOKING;
                conflicts.add(conflict(resource, start, end, type, overlap.getUuid(), overlap.getJobUuid(),
                        String.format("Venue '%s' is already reserved from %s to %s",
                                resource.getName(), overlap.getStartTime(), overlap.getEndTime())));
            }
            return;
        }

        int reserved = overlaps.stream().mapToInt(b -> b.getQuantity() == null ? 1 : b.getQuantity()).sum();
        int total = resource.getTotalQuantity() == null ? 0 : resource.getTotalQuantity();
        if (reserved + quantity > total) {
            conflicts.add(conflict(resource, start, end, ResourceConflictType.INSUFFICIENT_QUANTITY, null, null,
                    String.format("Equipment pool '%s' has %d of %d units free for this window but %d were requested",
                            resource.getName(), Math.max(0, total - reserved), total, quantity)));
        }
    }

    private String blackoutDescription(OrganisationResource resource, ResourceAvailabilityRule rule) {
        String notes = rule.getNotes() == null || rule.getNotes().isBlank() ? "" : " (" + rule.getNotes() + ")";
        return String.format("Window intersects a blackout of '%s'%s", resource.getName(), notes);
    }

    private boolean ruleAppliesOn(ResourceAvailabilityRule rule, LocalDate date) {
        return AvailabilityRuleSupport.ruleAppliesOn(rule, date);
    }

    private ResourceConflictDetail conflict(OrganisationResource resource,
                                            LocalDateTime start,
                                            LocalDateTime end,
                                            ResourceConflictType type,
                                            UUID conflictingBookingUuid,
                                            UUID conflictingJobUuid,
                                            String description) {
        return new ResourceConflictDetail(
                resource.getUuid(), resource.getName(), start, end, type,
                conflictingBookingUuid, conflictingJobUuid, description);
    }

    // ===== Loading helpers =====

    /**
     * Loads (optionally locking) every requested resource, verifying existence,
     * organisation ownership and per-type quantity shape.
     */
    private Map<UUID, OrganisationResource> loadAndVerifyResources(UUID organisationUuid,
                                                                   List<ResourceBookingRequest> requests,
                                                                   boolean lock) {
        Set<UUID> uuids = new LinkedHashSet<>();
        for (ResourceBookingRequest request : requests) {
            if (request.resourceUuid() == null) {
                throw new IllegalArgumentException("Every resource booking request requires a resource_uuid");
            }
            if (request.windows() == null || request.windows().isEmpty()) {
                throw new IllegalArgumentException("Every resource booking request requires at least one occurrence window");
            }
            uuids.add(request.resourceUuid());
        }

        List<OrganisationResource> loaded = lock
                ? resourceRepository.lockByUuids(uuids)
                : resourceRepository.findByUuidIn(uuids);
        Map<UUID, OrganisationResource> byUuid = loaded.stream()
                .collect(Collectors.toMap(OrganisationResource::getUuid, Function.identity()));

        for (UUID uuid : uuids) {
            OrganisationResource resource = byUuid.get(uuid);
            if (resource == null) {
                throw new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND_TEMPLATE, uuid));
            }
            if (organisationUuid != null && !organisationUuid.equals(resource.getOrganisationUuid())) {
                throw new IllegalArgumentException(String.format(
                        "Resource %s does not belong to organisation %s", uuid, organisationUuid));
            }
        }

        for (ResourceBookingRequest request : requests) {
            OrganisationResource resource = byUuid.get(request.resourceUuid());
            if (resource.getResourceType() == ResourceType.VENUE && request.quantity() != 1) {
                throw new IllegalArgumentException(String.format(
                        "Venue '%s' must be booked with quantity 1", resource.getName()));
            }
            if (request.quantity() < 1) {
                throw new IllegalArgumentException("Booking quantity must be at least 1");
            }
        }
        return byUuid;
    }

    private Map<UUID, OrganisationResource> lockResources(Set<UUID> resourceUuids) {
        return resourceRepository.lockByUuids(resourceUuids).stream()
                .collect(Collectors.toMap(OrganisationResource::getUuid, Function.identity()));
    }

    private Map<UUID, List<ResourceAvailabilityRule>> loadRules(Set<UUID> resourceUuids) {
        return ruleRepository.findByResourceUuidIn(resourceUuids).stream()
                .collect(Collectors.groupingBy(ResourceAvailabilityRule::getResourceUuid));
    }

    private OrganisationResource requireResource(UUID resourceUuid) {
        return resourceRepository.findByUuid(resourceUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND_TEMPLATE, resourceUuid)));
    }
}
