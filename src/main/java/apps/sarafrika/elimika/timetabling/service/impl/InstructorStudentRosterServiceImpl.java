package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import apps.sarafrika.elimika.timetabling.dto.InstructorClassOptionDTO;
import apps.sarafrika.elimika.timetabling.dto.InstructorStudentDTO;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.service.InstructorStudentRosterService;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.util.ScheduleSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorStudentRosterServiceImpl implements InstructorStudentRosterService {

    static final int MAX_PAGE_SIZE = 100;

    private final EnrollmentRepository enrollmentRepository;
    private final TrainingBranchLookupService trainingBranchLookupService;
    private final DomainSecurityService domainSecurityService;

    @Override
    public InstructorStudentRoster listInstructorStudents(UUID organisationUuid,
                                                          UUID instructorUuid,
                                                          String search,
                                                          UUID classDefinitionUuid,
                                                          int page,
                                                          int size) {
        requireOrganisationManager(organisationUuid);

        Page<Object[]> rows = enrollmentRepository.findInstructorStudentsForOrganisation(organisationUuid,
                instructorUuid, classDefinitionUuid, namePattern(search),
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
        Map<UUID, String> branchNames = branchNames(rows.getContent());
        Page<InstructorStudentDTO> students = rows.map(row -> toStudent(row, branchNames));

        List<InstructorClassOptionDTO> classOptions = enrollmentRepository
                .findInstructorClassOptionsForOrganisation(organisationUuid, instructorUuid).stream()
                .map(row -> new InstructorClassOptionDTO(toUuid(row[0]), (String) row[1]))
                .toList();
        long studentCount = enrollmentRepository.countInstructorStudentsForOrganisation(organisationUuid, instructorUuid);
        return new InstructorStudentRoster(students, classOptions, studentCount);
    }

    private void requireOrganisationManager(UUID organisationUuid) {
        if (domainSecurityService.isPlatformAdmin() || domainSecurityService.managesOrganisation(organisationUuid)) {
            return;
        }
        throw new AccessDeniedException(String.format(
                "Only managers of organisation %s can list the students its instructors teach.", organisationUuid));
    }

    /** A case-insensitive "contains" pattern, with LIKE wildcards in the search taken literally. */
    private static String namePattern(String search) {
        if (search == null || search.isBlank()) {
            return "%";
        }
        String literal = search.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + literal + "%";
    }

    private Map<UUID, String> branchNames(List<Object[]> rows) {
        List<UUID> branchUuids = rows.stream()
                .map(row -> toUuid(row[7]))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return branchUuids.isEmpty() ? Map.of() : trainingBranchLookupService.findBranchNames(branchUuids);
    }

    private static InstructorStudentDTO toStudent(Object[] row, Map<UUID, String> branchNames) {
        UUID branchUuid = toUuid(row[7]);
        long attended = ((Number) row[9]).longValue();
        long recorded = ((Number) row[10]).longValue();
        return new InstructorStudentDTO(
                toUuid(row[0]),
                (String) row[1],
                toUuid(row[2]),
                (String) row[3],
                (String) row[4],
                row[5] == null ? null : SessionFormat.valueOf(row[5].toString().toUpperCase(Locale.ROOT)),
                row[6] == null ? null : LocationType.fromValue(row[6].toString()),
                ScheduleSummary.fromPackedTemplates((String) row[12]),
                branchUuid,
                branchUuid == null ? null : branchNames.get(branchUuid),
                toLocalDateTime(row[8]),
                recorded == 0 ? null : Math.round(attended * 1000d / recorded) / 10d,
                EnrollmentStatus.fromValue((String) row[11]));
    }

    private static UUID toUuid(Object value) {
        return switch (value) {
            case null -> null;
            case UUID uuid -> uuid;
            default -> UUID.fromString(value.toString());
        };
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        return switch (value) {
            case null -> null;
            case java.sql.Timestamp timestamp -> timestamp.toLocalDateTime();
            case OffsetDateTime offsetDateTime -> offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            case Instant instant -> LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            case LocalDateTime localDateTime -> localDateTime;
            default -> null;
        };
    }
}
