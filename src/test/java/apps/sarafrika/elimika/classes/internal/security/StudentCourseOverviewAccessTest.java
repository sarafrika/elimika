package apps.sarafrika.elimika.classes.internal.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCourseOverviewAccessTest {

    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID CALLER = UUID.randomUUID();

    @Mock private DomainSecurityService domainSecurityService;
    @Mock private LearnerProfileLookupService learnerProfileLookupService;

    private StudentCourseOverviewAccess access;

    @BeforeEach
    void setUp() {
        access = new StudentCourseOverviewAccess(domainSecurityService, learnerProfileLookupService);
    }

    @Test
    void theLearnerMayRead() {
        when(domainSecurityService.isStudentWithUuid(STUDENT)).thenReturn(true);
        assertThat(access.canRead(STUDENT)).isTrue();
    }

    @Test
    void aPlatformAdminMayRead() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);
        assertThat(access.canRead(STUDENT)).isTrue();
    }

    @Test
    void anAcademicGuardianMayRead() {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(CALLER);
        when(learnerProfileLookupService.guardianCanViewAcademics(CALLER, STUDENT)).thenReturn(true);
        assertThat(access.canRead(STUDENT)).isTrue();
    }

    @Test
    void anyoneElseIsRefused() {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(CALLER);
        assertThat(access.canRead(STUDENT)).isFalse();
        assertThat(access.canRead(null)).isFalse();
    }
}
