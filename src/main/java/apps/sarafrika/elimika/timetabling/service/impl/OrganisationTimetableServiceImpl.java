package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.timetabling.dto.OrganisationTimetableEntryDTO;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.service.OrganisationTimetableService;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganisationTimetableServiceImpl implements OrganisationTimetableService {

    private final ScheduledInstanceRepository scheduledInstanceRepository;
    private final InstructorLookupService instructorLookupService;

    @Override
    public List<OrganisationTimetableEntryDTO> getOrganisationTimetable(UUID organisationUuid,
                                                                        LocalDate start,
                                                                        LocalDate end) {
        validate(organisationUuid, start, end);

        List<Object[]> rows = scheduledInstanceRepository.findOrganisationSessionsInRange(
                organisationUuid, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        if (rows.isEmpty()) {
            return List.of();
        }

        // One batched directory lookup for the few instructors behind many sessions.
        Set<UUID> instructorUuids = rows.stream()
                .map(row -> toUuid(row[3]))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, InstructorDirectoryEntry> instructors =
                instructorLookupService.findInstructorDirectoryEntries(instructorUuids);

        return rows.stream().map(row -> toEntry(row, instructors)).toList();
    }

    private static void validate(UUID organisationUuid, LocalDate start, LocalDate end) {
        if (organisationUuid == null) {
            throw new IllegalArgumentException("Organisation UUID cannot be null");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
    }

    private static OrganisationTimetableEntryDTO toEntry(Object[] row, Map<UUID, InstructorDirectoryEntry> instructors) {
        UUID instructorUuid = toUuid(row[3]);
        InstructorDirectoryEntry instructor = instructorUuid == null ? null : instructors.get(instructorUuid);
        return new OrganisationTimetableEntryDTO(
                toUuid(row[0]),
                toUuid(row[1]),
                (String) row[2],
                instructorUuid,
                instructor == null ? null : instructor.displayName(),
                toLocalDateTime(row[4]),
                toLocalDateTime(row[5]),
                (String) row[6],
                (String) row[7],
                (String) row[8],
                row[9] == null ? null : ((Number) row[9]).intValue(),
                row[10] == null ? null : SchedulingStatus.fromValue(row[10].toString()),
                row[11] == null ? 0L : ((Number) row[11]).longValue());
    }

    private static UUID toUuid(Object value) {
        return switch (value) {
            case null -> null;
            case UUID uuid -> uuid;
            default -> UUID.fromString(value.toString());
        };
    }

    // Same zone the entity path reads timestamptz in, so these times match ScheduledInstanceDTO's.
    private static LocalDateTime toLocalDateTime(Object value) {
        return switch (value) {
            case null -> null;
            case java.sql.Timestamp timestamp -> timestamp.toLocalDateTime();
            case OffsetDateTime offsetDateTime -> offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            case Instant instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            case LocalDateTime localDateTime -> localDateTime;
            default -> null;
        };
    }
}
