package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.student.internal.LearnerSkillGoalStore;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.spi.StudentGuardianLookupService;
import apps.sarafrika.elimika.student.spi.StudentGuardianLookupService.GuardianStudentAccess;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Learner profile lookup for recommendations")
class LearnerProfileLookupServiceImplTest {

    @Mock private StudentRepository studentRepository;
    @Mock private UserLookupService userLookupService;
    @Mock private StudentGuardianLookupService studentGuardianLookupService;
    @Mock private LearnerSkillGoalStore skillGoalStore;
    @InjectMocks private LearnerProfileLookupServiceImpl service;

    @Test
    @DisplayName("a guardian with a FULL or ACADEMICS share may see academics; ATTENDANCE or no link may not")
    void guardianScopes() {
        UUID guardian = UUID.randomUUID();
        UUID full = UUID.randomUUID();
        UUID academics = UUID.randomUUID();
        UUID attendance = UUID.randomUUID();
        when(studentGuardianLookupService.findActiveGuardianStudents(guardian)).thenReturn(List.of(
                new GuardianStudentAccess(full, GuardianShareScope.FULL),
                new GuardianStudentAccess(academics, GuardianShareScope.ACADEMICS),
                new GuardianStudentAccess(attendance, GuardianShareScope.ATTENDANCE)));

        assertThat(service.guardianCanViewAcademics(guardian, full)).isTrue();
        assertThat(service.guardianCanViewAcademics(guardian, academics)).isTrue();
        assertThat(service.guardianCanViewAcademics(guardian, attendance)).isFalse();
        assertThat(service.guardianCanViewAcademics(guardian, UUID.randomUUID())).isFalse();
        assertThat(service.guardianCanViewAcademics(null, full)).isFalse();
    }

    @Test
    @DisplayName("the age comes from tenancy as a number; the date of birth is never read here")
    void ageFromTenancy() {
        UUID studentUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        Student student = new Student();
        student.setUuid(studentUuid);
        student.setUserUuid(userUuid);
        LocalDate today = LocalDate.of(2026, 9, 30);
        when(studentRepository.findByUuid(studentUuid)).thenReturn(Optional.of(student));
        when(userLookupService.findUserAgeInYears(userUuid, today)).thenReturn(OptionalInt.of(12));

        assertThat(service.findLearnerAge(studentUuid, today)).hasValue(12);
        assertThat(service.findLearnerAge(null, today)).isEmpty();
    }
}
