package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.resourcing.spi.InstanceWindow;
import apps.sarafrika.elimika.shared.utils.recurrence.OccurrenceWindow;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.timetabling.factory.InstructorTimeHoldFactory;
import apps.sarafrika.elimika.timetabling.model.InstructorTimeHold;
import apps.sarafrika.elimika.timetabling.repository.InstructorTimeHoldRepository;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldDTO;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldRequest;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldService;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldStatus;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InstructorTimeHoldServiceImpl implements InstructorTimeHoldService {

    /** States that still occupy a diary; everything else is terminal. */
    private static final List<InstructorTimeHoldStatus> ACTIVE_STATUSES =
            List.of(InstructorTimeHoldStatus.TENTATIVE, InstructorTimeHoldStatus.FIRM);

    /** Only a hire makes a claim exclusive, so only FIRM ever counts as a clash. */
    private static final List<InstructorTimeHoldStatus> BLOCKING_STATUSES =
            List.of(InstructorTimeHoldStatus.FIRM);

    private static final String DEFAULT_TIMEZONE = "UTC";

    private final InstructorTimeHoldRepository holdRepository;
    private final OrganisationLookupService organisationLookupService;

    @Override
    public void holdForApplication(InstructorTimeHoldRequest request) {
        validateHoldRequest(request);
        if (request.windows() == null || request.windows().isEmpty()) {
            log.debug("No occurrence windows to hold for application {}", request.applicationUuid());
            return;
        }

        releaseHoldsForApplication(request.applicationUuid(), "Replaced by an updated hold");
        writeHolds(request, InstructorTimeHoldStatus.TENTATIVE);
    }

    @Override
    public void firmOrCreateHoldsForApplication(InstructorTimeHoldRequest request) {
        validateHoldRequest(request);

        // An application written before this table existed holds nothing, so promoting alone would
        // leave the hire undefended. Anything it already holds is authoritative and just promoted.
        boolean holdsSomething = !holdRepository
                .findByApplicationUuidAndStatusIn(request.applicationUuid(), ACTIVE_STATUSES).isEmpty();
        if (holdsSomething) {
            firmHoldsForApplication(request.applicationUuid());
            return;
        }

        if (request.windows() == null || request.windows().isEmpty()) {
            log.debug("No occurrence windows to reserve for hired application {}", request.applicationUuid());
            return;
        }
        writeHolds(request, InstructorTimeHoldStatus.FIRM);
    }

    private void validateHoldRequest(InstructorTimeHoldRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("An instructor time hold request is required");
        }
        if (request.instructorUuid() == null) {
            throw new IllegalArgumentException("Instructor UUID is required to hold instructor time");
        }
        if (request.jobUuid() == null) {
            throw new IllegalArgumentException("Job UUID is required to hold instructor time");
        }
        if (request.applicationUuid() == null) {
            throw new IllegalArgumentException("Application UUID is required to hold instructor time");
        }
    }

    private void writeHolds(InstructorTimeHoldRequest request, InstructorTimeHoldStatus status) {
        String timezone = request.timezone() == null || request.timezone().isBlank()
                ? DEFAULT_TIMEZONE
                : request.timezone();

        List<InstructorTimeHold> holds = new ArrayList<>();
        for (OccurrenceWindow window : request.windows()) {
            if (window.start() == null || window.end() == null || !window.start().isBefore(window.end())) {
                throw new IllegalArgumentException("Instructor time holds require start_time before end_time");
            }
            InstructorTimeHold hold = new InstructorTimeHold();
            hold.setInstructorUuid(request.instructorUuid());
            hold.setInstructorUserUuid(request.instructorUserUuid());
            hold.setOrganisationUuid(request.organisationUuid());
            hold.setJobUuid(request.jobUuid());
            hold.setApplicationUuid(request.applicationUuid());
            hold.setTitle(request.title());
            hold.setStartTime(window.start());
            hold.setEndTime(window.end());
            hold.setTimezone(timezone);
            hold.setStatus(status);
            holds.add(hold);
        }
        holdRepository.saveAll(holds);
        log.info("Placed {} {} instructor time holds for application {} on job {}",
                holds.size(), status, request.applicationUuid(), request.jobUuid());
    }

    @Override
    public void firmHoldsForApplication(UUID applicationUuid) {
        List<InstructorTimeHold> holds = holdRepository.findByApplicationUuidAndStatusIn(
                applicationUuid, List.of(InstructorTimeHoldStatus.TENTATIVE));
        if (holds.isEmpty()) {
            return;
        }
        holds.forEach(hold -> hold.setStatus(InstructorTimeHoldStatus.FIRM));
        holdRepository.saveAll(holds);
        log.info("Firmed {} instructor time holds for application {}", holds.size(), applicationUuid);
    }

    @Override
    public void releaseHoldsForApplication(UUID applicationUuid, String reason) {
        List<InstructorTimeHold> holds =
                holdRepository.findByApplicationUuidAndStatusIn(applicationUuid, ACTIVE_STATUSES);
        release(holds, reason);
        if (!holds.isEmpty()) {
            log.info("Released {} instructor time holds for application {}: {}",
                    holds.size(), applicationUuid, reason);
        }
    }

    @Override
    public void releaseHoldsForJob(UUID jobUuid, String reason) {
        List<InstructorTimeHold> holds = holdRepository.findByJobUuidAndStatusIn(jobUuid, ACTIVE_STATUSES);
        release(holds, reason);
        if (!holds.isEmpty()) {
            log.info("Released {} instructor time holds for marketplace job {}: {}",
                    holds.size(), jobUuid, reason);
        }
    }

    @Override
    public void releaseHoldsForJobExcept(UUID jobUuid, UUID keepApplicationUuid, String reason) {
        List<InstructorTimeHold> holds = holdRepository.findByJobUuidAndStatusIn(jobUuid, ACTIVE_STATUSES)
                .stream()
                .filter(hold -> keepApplicationUuid == null || !keepApplicationUuid.equals(hold.getApplicationUuid()))
                .toList();
        release(holds, reason);
        if (!holds.isEmpty()) {
            log.info("Released {} instructor time holds on job {} outside application {}: {}",
                    holds.size(), jobUuid, keepApplicationUuid, reason);
        }
    }

    @Override
    public void confirmHoldsForJob(UUID jobUuid, UUID classDefinitionUuid, List<InstanceWindow> instanceWindows) {
        List<InstructorTimeHold> holds = holdRepository.findByJobUuidAndStatusIn(jobUuid, ACTIVE_STATUSES);
        if (holds.isEmpty()) {
            return;
        }

        List<InstanceWindow> windows = instanceWindows == null ? List.of() : instanceWindows;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int confirmed = 0;
        for (InstructorTimeHold hold : holds) {
            Optional<InstanceWindow> match = windows.stream()
                    .filter(w -> hold.getStartTime().equals(w.startTime()) && hold.getEndTime().equals(w.endTime()))
                    .findFirst();
            if (match.isPresent()) {
                hold.setStatus(InstructorTimeHoldStatus.CONFIRMED);
                hold.setClassDefinitionUuid(classDefinitionUuid);
                hold.setScheduledInstanceUuid(match.get().scheduledInstanceUuid());
                confirmed++;
            } else {
                hold.setStatus(InstructorTimeHoldStatus.RELEASED);
                hold.setReleasedAt(now);
                hold.setReleaseReason("Occurrence was not scheduled when the class was created");
            }
        }
        holdRepository.saveAll(holds);
        log.info("Confirmed {} of {} instructor time holds for marketplace job {} onto class {}",
                confirmed, holds.size(), jobUuid, classDefinitionUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorTimeHoldDTO> findActiveHolds(UUID instructorUuid, LocalDate start, LocalDate end) {
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        List<InstructorTimeHold> holds = holdRepository.findOverlappingHolds(
                instructorUuid, ACTIVE_STATUSES, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        return attributeToOrganisations(InstructorTimeHoldFactory.toDTOList(holds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorTimeHoldDTO> findBlockingHolds(UUID instructorUuid,
                                                         LocalDateTime from,
                                                         LocalDateTime to,
                                                         UUID excludingJobUuid) {
        if (instructorUuid == null || from == null || to == null || !from.isBefore(to)) {
            return List.of();
        }

        List<InstructorTimeHold> holds = holdRepository
                .findOverlappingHolds(instructorUuid, BLOCKING_STATUSES, from, to)
                .stream()
                .filter(hold -> excludingJobUuid == null || !excludingJobUuid.equals(hold.getJobUuid()))
                .toList();
        // Names are deliberately not resolved here. The caller reads windows only, and telling one
        // organisation which rival already holds the instructor would leak its recruitment.
        return InstructorTimeHoldFactory.toDTOList(holds);
    }

    private void release(List<InstructorTimeHold> holds, String reason) {
        if (holds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (InstructorTimeHold hold : holds) {
            hold.setStatus(InstructorTimeHoldStatus.RELEASED);
            hold.setReleasedAt(now);
            hold.setReleaseReason(reason);
        }
        holdRepository.saveAll(holds);
    }

    /**
     * Names the organisation behind each hold in one batched lookup, mirroring how a scheduled
     * session is attributed: held time an instructor never asked for should say whose work it is.
     */
    private List<InstructorTimeHoldDTO> attributeToOrganisations(List<InstructorTimeHoldDTO> holds) {
        Set<UUID> organisationUuids = holds.stream()
                .map(InstructorTimeHoldDTO::organisationUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (organisationUuids.isEmpty()) {
            return holds;
        }

        Map<UUID, String> organisationNames = organisationLookupService.findOrganisationNames(organisationUuids);
        if (organisationNames.isEmpty()) {
            return holds;
        }

        return holds.stream()
                .map(hold -> hold.organisationUuid() == null
                        ? hold
                        : hold.withOrganisationName(organisationNames.get(hold.organisationUuid())))
                .toList();
    }
}
