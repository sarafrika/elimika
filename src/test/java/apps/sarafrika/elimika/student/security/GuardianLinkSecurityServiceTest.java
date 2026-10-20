package apps.sarafrika.elimika.student.security;

import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuardianLinkSecurityServiceTest {

    private final DomainSecurityService domainSecurity = mock();
    private final StudentGuardianLinkRepository links = mock();
    private final LearnerProgressLookupService learnerProgress = mock();
    private final GuardianLinkSecurityService security = new GuardianLinkSecurityService(domainSecurity, links, learnerProgress);

    private final UUID guardian = UUID.randomUUID();
    private final UUID student = UUID.randomUUID();
    private final UUID enrollment = UUID.randomUUID();

    @BeforeEach
    void signedInParent() {
        when(domainSecurity.getCurrentUserUuid()).thenReturn(guardian);
        when(domainSecurity.hasAnyDomain(UserDomain.parent)).thenReturn(true);
        when(learnerProgress.findStudentUuidOfCourseEnrollment(enrollment)).thenReturn(Optional.of(student));
    }

    @Test
    @DisplayName("a parent with an active link that shares academics may read the child's marks")
    void activeAcademicLinkMayRead() {
        when(links.existsByStudentUuidAndGuardianUserUuidAndStatusAndShareScopeIn(
                student, guardian, GuardianLinkStatus.ACTIVE, List.of(GuardianShareScope.FULL, GuardianShareScope.ACADEMICS)))
                .thenReturn(true);
        assertThat(security.canViewEnrollmentAcademics(enrollment)).isTrue();
    }

    @Test
    @DisplayName("no academic link (attendance-only, revoked or none) means no marks")
    void withoutAnAcademicLinkNoMarks() {
        when(links.existsByStudentUuidAndGuardianUserUuidAndStatusAndShareScopeIn(eq(student), eq(guardian), any(), any()))
                .thenReturn(false);
        assertThat(security.canViewEnrollmentAcademics(enrollment)).isFalse();
    }

    @Test
    @DisplayName("someone without the parent domain, or an unknown enrolment, is refused")
    void nonParentsAndUnknownEnrolmentsAreRefused() {
        when(domainSecurity.hasAnyDomain(UserDomain.parent)).thenReturn(false);
        assertThat(security.canViewEnrollmentAcademics(enrollment)).isFalse();

        when(domainSecurity.hasAnyDomain(UserDomain.parent)).thenReturn(true);
        UUID unknown = UUID.randomUUID();
        when(learnerProgress.findStudentUuidOfCourseEnrollment(unknown)).thenReturn(Optional.empty());
        assertThat(security.canViewEnrollmentAcademics(unknown)).isFalse();
    }
}
