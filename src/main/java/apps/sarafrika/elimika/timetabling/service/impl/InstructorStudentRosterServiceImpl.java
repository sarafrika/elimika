package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The students an instructor teaches in an organisation's classes.
 * <p>
 * <b>Name search.</b> Free text is served only by search, never by SQL {@code LIKE}. The roster query
 * runs without a name filter - it is already authorised in SQL (organisation, instructor of record,
 * optional class) - and its student user UUIDs become an explicit scope on the {@code people} index:
 * {@code uuid IN [roster user uuids]}, matched on names only (no email, username or user number, as for
 * an organisation manager's roster search). The ranked hits are mapped back onto the roster rows and
 * paged in memory. Timetabling reaches the index through {@code shared.search.SearchGateway}, which is
 * in the {@code shared} module it may already depend on; the scope is built from rows the caller is
 * allowed to see, so the index can only narrow the roster, never widen it. Search off, the
 * {@code people} index's reads off, or the engine failing is a 503 ({@link SearchUnavailableException}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorStudentRosterServiceImpl implements InstructorStudentRosterService {

    static final int MAX_PAGE_SIZE = 100;

    /** The {@code people} index, owned by the tenancy module; its documents are keyed by user UUID. */
    static final String PEOPLE_INDEX = "people";
    /** Name attributes of a people document; a roster search never matches email or username. */
    static final List<String> NAME_ATTRIBUTES = List.of("full_name", "first_name", "last_name");
    private static final int USER_UUID_COLUMN = 13;

    private final EnrollmentRepository enrollmentRepository;
    private final TrainingBranchLookupService trainingBranchLookupService;
    private final DomainSecurityService domainSecurityService;
    private final SearchGateway searchGateway;
    private final SearchAvailability searchAvailability;

    @Override
    public InstructorStudentRoster listInstructorStudents(UUID organisationUuid,
                                                          UUID instructorUuid,
                                                          String search,
                                                          UUID classDefinitionUuid,
                                                          int page,
                                                          int size) {
        requireOrganisationManager(organisationUuid);

        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        Page<Object[]> rows = StringUtils.hasText(search)
                ? searchRoster(organisationUuid, instructorUuid, classDefinitionUuid, search.trim(), pageRequest)
                : enrollmentRepository.findInstructorStudentsForOrganisation(organisationUuid,
                        instructorUuid, classDefinitionUuid, pageRequest);
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

    /**
     * The roster rows of the students whose names match {@code text}, in the index's rank order (a
     * student's classes stay together, in the roster's class order), paged in memory.
     */
    private Page<Object[]> searchRoster(UUID organisationUuid, UUID instructorUuid, UUID classDefinitionUuid,
                                        String text, PageRequest pageRequest) {
        if (!searchAvailability.isReadEnabled(PEOPLE_INDEX)) {
            throw new SearchUnavailableException("Search is not enabled for " + PEOPLE_INDEX);
        }
        Map<UUID, List<Object[]>> rowsByUser = new LinkedHashMap<>();
        for (Object[] row : enrollmentRepository.findAllInstructorStudentsForOrganisation(
                organisationUuid, instructorUuid, classDefinitionUuid)) {
            UUID userUuid = toUuid(row[USER_UUID_COLUMN]);
            if (userUuid != null) {
                rowsByUser.computeIfAbsent(userUuid, key -> new ArrayList<>()).add(row);
            }
        }
        if (rowsByUser.isEmpty()) {
            return new PageImpl<>(List.of(), pageRequest, 0);
        }
        List<Object[]> matched = new ArrayList<>();
        for (UUID userUuid : rankedMatches(text, rowsByUser.keySet(), organisationUuid, instructorUuid)) {
            matched.addAll(rowsByUser.getOrDefault(userUuid, List.of()));
        }
        int from = (int) Math.min(pageRequest.getOffset(), matched.size());
        int to = Math.min(from + pageRequest.getPageSize(), matched.size());
        return new PageImpl<>(matched.subList(from, to), pageRequest, matched.size());
    }

    /** Every roster user the index matches, in rank order, a page of {@value SearchRequest#MAX_SIZE} at a time. */
    private List<UUID> rankedMatches(String text, Collection<UUID> rosterUserUuids,
                                     UUID organisationUuid, UUID instructorUuid) {
        SearchScope scope = SearchScope.of(SearchFilter.in("uuid", rosterUserUuids),
                "instructor-roster:org:" + organisationUuid + ":instructor:" + instructorUuid);
        Set<UUID> ranked = new LinkedHashSet<>();
        for (int page = 0; ranked.size() < rosterUserUuids.size(); page++) {
            SearchPage result = searchGateway.search(new SearchRequest(PEOPLE_INDEX, text, null, scope, List.of(),
                    page, SearchRequest.MAX_SIZE, List.of(), NAME_ATTRIBUTES));
            result.hits().stream().map(SearchHit::uuid).filter(Objects::nonNull).forEach(ranked::add);
            if (result.hits().size() < SearchRequest.MAX_SIZE || (long) (page + 1) * SearchRequest.MAX_SIZE >= result.totalHits()) {
                break;
            }
        }
        return List.copyOf(ranked);
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
