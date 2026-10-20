package apps.sarafrika.elimika.commerce.internal.service;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.student.spi.StudentGuardianLookupService;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Who a seat is for. A buyer buys for themselves unless a line names a learner; a guardian may name a
 * child they hold an active link to. Eligibility and seat holds are then judged for that learner.
 */
@Component
@RequiredArgsConstructor
public class LearnerResolver {

    public static final String STUDENT_KEY = "student_uuid";

    private final DomainSecurityService domainSecurityService;
    private final StudentGuardianLookupService studentGuardianLookupService;
    private final StudentLookupService studentLookupService;

    /** Checks the caller may buy for {@code studentUuid}; null means "for myself" and is always allowed. */
    public UUID authorise(UUID studentUuid) {
        if (studentUuid == null) {
            return null;
        }
        UUID caller = domainSecurityService.getCurrentUserUuid();
        boolean ownProfile = caller != null && studentLookupService.findStudentUuidByUserUuid(caller)
                .filter(studentUuid::equals)
                .isPresent();
        boolean guardian = caller != null && studentGuardianLookupService.findActiveGuardianStudents(caller).stream()
                .anyMatch(access -> studentUuid.equals(access.studentUuid()));
        if (!ownProfile && !guardian && !domainSecurityService.isPlatformAdmin()) {
            throw new AccessDeniedException("You can only buy a seat for yourself or a learner you are the guardian of");
        }
        return studentUuid;
    }

    /** The learner a line names, if any. */
    public UUID learnerOf(Map<String, Object> metadata) {
        Object value = metadata == null ? null : metadata.get(STUDENT_KEY);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    /** The user whose eligibility a line is judged by: the named learner's account, else the buyer. */
    public UUID learnerUserUuid(Map<String, Object> metadata, UUID buyerUserUuid) {
        UUID learner = learnerOf(metadata);
        if (learner == null) {
            return buyerUserUuid;
        }
        return studentLookupService.getStudentUserUuid(learner)
                .orElseThrow(() -> new IllegalStateException("The learner this seat is for has no account"));
    }
}
