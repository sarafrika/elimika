package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.security.StudentDirectorySecurityService;
import apps.sarafrika.elimika.student.security.StudentDirectorySecurityService.DirectoryScope;
import apps.sarafrika.elimika.student.service.StudentDirectoryService;
import apps.sarafrika.elimika.student.service.StudentService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Applies the directory projection to whatever {@link StudentService} returns.
 * <p>
 * Every route funnels through {@link StudentDirectorySecurityService#project(StudentDTO)}, including
 * the single-record one, so there is exactly one place where a guardian contact can leave this
 * module and exactly one rule deciding whether it may. The caller's relationships are resolved once
 * per request, so projecting a page costs no queries beyond the page itself.
 * <p>
 * Listing and search are also <em>scoped</em>: only the records the caller is related to (see
 * {@link StudentDirectorySecurityService#directoryScope()}) are ever queried, so a caller with no
 * relationship to any learner gets an empty page rather than the whole directory, and page totals
 * never count rows the caller cannot see.
 * <p>
 * The one exception is an explicit-identifier lookup — a request whose only filters name records
 * by {@code uuid} or {@code user_uuid} (at most {@value #MAX_EXPLICIT_IDS} of them). Rosters, review
 * cards and profile pages resolve display names that way for learners the viewer has no
 * relationship with. Such a lookup skips the scope but is still projected, so an unrelated caller
 * gets display identity only, and it cannot enumerate: the caller must already hold the
 * identifiers.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
@Service
@RequiredArgsConstructor
public class StudentDirectoryServiceImpl implements StudentDirectoryService {

    static final int MAX_EXPLICIT_IDS = 100;

    private static final Set<String> PAGING_PARAMS = Set.of("page", "size", "sort");
    private static final Set<String> IDENTIFIER_FIELDS = Set.of("uuid", "useruuid");
    private static final Set<String> IDENTIFIER_OPERATIONS = Set.of("eq", "in");

    private final StudentService studentService;
    private final StudentDirectorySecurityService studentDirectorySecurityService;

    @Override
    public StudentDTO getStudent(UUID uuid) {
        return studentDirectorySecurityService.project(studentService.getStudentByUuId(uuid));
    }

    @Override
    public Page<StudentDTO> listStudents(Pageable pageable) {
        return searchStudents(Map.of(), pageable);
    }

    @Override
    public Page<StudentDTO> searchStudents(Map<String, String> searchParams, Pageable pageable) {
        if (isExplicitIdentifierLookup(searchParams)) {
            return studentService.search(searchParams, pageable, null)
                    .map(studentDirectorySecurityService::project);
        }
        DirectoryScope scope = studentDirectorySecurityService.directoryScope();
        if (scope.isEmpty()) {
            return Page.empty(pageable);
        }
        return studentService.search(searchParams, pageable, toSpecification(scope))
                .map(studentDirectorySecurityService::project);
    }

    /**
     * True when every non-paging filter names records by {@code uuid} or {@code user_uuid} (snake or
     * camel case, bare or with {@code _eq}/{@code _in}) and at least one does. Mixing in any other
     * filter makes it a search, which stays scoped.
     *
     * @throws IllegalArgumentException when the lookup names more than {@value #MAX_EXPLICIT_IDS}
     *                                  identifiers
     */
    private static boolean isExplicitIdentifierLookup(Map<String, String> searchParams) {
        if (searchParams == null || searchParams.isEmpty()) {
            return false;
        }
        int identifiers = 0;
        for (Map.Entry<String, String> param : searchParams.entrySet()) {
            String key = param.getKey().toLowerCase(Locale.ROOT);
            String value = param.getValue();
            if (PAGING_PARAMS.contains(key) || value == null || value.isEmpty()) {
                continue;
            }
            if (!isIdentifierKey(key)) {
                return false;
            }
            for (String id : value.split(",")) {
                if (!id.isBlank()) {
                    identifiers++;
                }
            }
        }
        if (identifiers > MAX_EXPLICIT_IDS) {
            throw new IllegalArgumentException(
                    "A student lookup may name at most " + MAX_EXPLICIT_IDS + " identifiers");
        }
        return identifiers > 0;
    }

    private static boolean isIdentifierKey(String key) {
        String field = key;
        int lastUnderscore = key.lastIndexOf('_');
        if (lastUnderscore > 0 && IDENTIFIER_OPERATIONS.contains(key.substring(lastUnderscore + 1))) {
            field = key.substring(0, lastUnderscore);
        }
        return IDENTIFIER_FIELDS.contains(field.replace("_", ""));
    }

    /**
     * The scope as a query predicate: null when unrestricted, otherwise "student uuid in the
     * caller's learners OR owning user in the caller's organisations' members".
     */
    private static Specification<Student> toSpecification(DirectoryScope scope) {
        if (scope.unrestricted()) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> arms = new ArrayList<>(2);
            if (!scope.studentUuids().isEmpty()) {
                arms.add(root.get("uuid").in(scope.studentUuids()));
            }
            if (!scope.userUuids().isEmpty()) {
                arms.add(root.get("userUuid").in(scope.userUuids()));
            }
            return cb.or(arms.toArray(Predicate[]::new));
        };
    }
}
