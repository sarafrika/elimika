package apps.sarafrika.elimika.student.internal.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.student.spi.StudentSecuritySpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Internal implementation of student security operations.
 * Provides authorization checks for student identity.
 * <p>
 * The JWT → user → student walk this used to perform by hand is the same question
 * {@link DomainSecurityService#getCurrentStudentUuid()} already answers, and answers once per
 * request. Delegating keeps the two implementations of the predicate from drifting apart in either
 * behaviour or cost.
 *
 * @author Wilfred Njuguna
 * @version 1.2
 * @since 2025-10-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentSecurityServiceImpl implements StudentSecuritySpi {

    private final DomainSecurityService domainSecurityService;

    /**
     * Checks if the currently authenticated user belongs to a specific student.
     *
     * @param studentUuid UUID of the student to check
     * @return true if the current user is the specified student, false otherwise
     */
    @Override
    public boolean isStudentWithUuid(UUID studentUuid) {
        if (studentUuid == null) {
            return false;
        }
        boolean isStudent = studentUuid.equals(domainSecurityService.getCurrentStudentUuid());
        log.debug("Student identity check against target {}: {}", studentUuid, isStudent);
        return isStudent;
    }
}
