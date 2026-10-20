package apps.sarafrika.elimika.commerce.internal.service;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.student.spi.StudentGuardianLookupService;
import apps.sarafrika.elimika.student.spi.StudentGuardianLookupService.GuardianStudentAccess;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("A seat is for the buyer, or a learner the buyer guards")
class LearnerResolverTest {

    private static final UUID GUARDIAN = UUID.randomUUID();
    private static final UUID CHILD = UUID.randomUUID();
    private static final UUID CHILD_USER = UUID.randomUUID();

    @Mock
    private DomainSecurityService security;
    @Mock
    private StudentGuardianLookupService guardians;
    @Mock
    private StudentLookupService students;

    private LearnerResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new LearnerResolver(security, guardians, students);
    }

    @Test
    @DisplayName("buying for yourself needs no check")
    void forYourself() {
        assertThat(resolver.authorise(null)).isNull();
    }

    @Test
    @DisplayName("a guardian with an active link may buy for the child")
    void guardianMayBuy() {
        when(security.getCurrentUserUuid()).thenReturn(GUARDIAN);
        when(students.findStudentUuidByUserUuid(GUARDIAN)).thenReturn(Optional.empty());
        when(guardians.findActiveGuardianStudents(GUARDIAN)).thenReturn(List.of(new GuardianStudentAccess(CHILD, null)));

        assertThat(resolver.authorise(CHILD)).isEqualTo(CHILD);
    }

    @Test
    @DisplayName("anyone else is refused")
    void strangerIsRefused() {
        when(security.getCurrentUserUuid()).thenReturn(GUARDIAN);
        when(students.findStudentUuidByUserUuid(GUARDIAN)).thenReturn(Optional.empty());
        when(guardians.findActiveGuardianStudents(GUARDIAN)).thenReturn(List.of());
        when(security.isPlatformAdmin()).thenReturn(false);

        assertThatThrownBy(() -> resolver.authorise(CHILD)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("a line for a child is judged by the child's account; a plain line by the buyer's")
    void learnerUser() {
        when(students.getStudentUserUuid(CHILD)).thenReturn(Optional.of(CHILD_USER));

        assertThat(resolver.learnerUserUuid(Map.of("student_uuid", CHILD.toString()), GUARDIAN)).isEqualTo(CHILD_USER);
        assertThat(resolver.learnerUserUuid(Map.of(), GUARDIAN)).isEqualTo(GUARDIAN);
    }
}
