package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Minor learner lookup")
class MinorLearnerLookupServiceImplTest {

    @Mock private StudentRepository studentRepository;
    @Mock private UserLookupService userLookupService;
    @InjectMocks private MinorLearnerLookupServiceImpl service;

    @Test
    @DisplayName("maps students to users and asks tenancy for users born after the 18-year cutoff")
    void resolvesMinorsThroughTheUserSpi() {
        UUID minorStudent = UUID.randomUUID();
        UUID adultStudent = UUID.randomUUID();
        UUID minorUser = UUID.randomUUID();
        UUID adultUser = UUID.randomUUID();
        when(studentRepository.findByUuidIn(anyCollection()))
                .thenReturn(List.of(student(minorStudent, minorUser), student(adultStudent, adultUser)));
        LocalDate asOf = LocalDate.of(2026, 9, 30);
        when(userLookupService.findUserUuidsBornAfter(anyCollection(), eq(LocalDate.of(2008, 9, 30))))
                .thenReturn(Set.of(minorUser));

        assertThat(service.findMinorStudentUuids(List.of(minorStudent, adultStudent), asOf))
                .containsExactly(minorStudent);
    }

    @Test
    @DisplayName("nothing to test means no lookups")
    void emptyInput() {
        assertThat(service.findMinorStudentUuids(List.of(), LocalDate.now())).isEmpty();
        verifyNoInteractions(studentRepository, userLookupService);
    }

    @Test
    @DisplayName("a student with no user, like a user with no date of birth, is not a minor")
    void unknownUsersAreAdults() {
        UUID orphan = UUID.randomUUID();
        when(studentRepository.findByUuidIn(any())).thenReturn(List.of(student(orphan, null)));

        assertThat(service.findMinorStudentUuids(List.of(orphan), LocalDate.now())).isEmpty();
        verifyNoInteractions(userLookupService);
    }

    private static Student student(UUID uuid, UUID userUuid) {
        Student student = new Student();
        student.setUuid(uuid);
        student.setUserUuid(userUuid);
        return student;
    }
}
