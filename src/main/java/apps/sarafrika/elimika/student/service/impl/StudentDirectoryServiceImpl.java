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
import java.util.Map;
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
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
@Service
@RequiredArgsConstructor
public class StudentDirectoryServiceImpl implements StudentDirectoryService {

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
        DirectoryScope scope = studentDirectorySecurityService.directoryScope();
        if (scope.isEmpty()) {
            return Page.empty(pageable);
        }
        return studentService.search(searchParams, pageable, toSpecification(scope))
                .map(studentDirectorySecurityService::project);
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
