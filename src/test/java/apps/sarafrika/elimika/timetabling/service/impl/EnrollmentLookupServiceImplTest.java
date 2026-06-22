package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService.ClassEnrollmentStatusSnapshot;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentLookupServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private EnrollmentLookupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EnrollmentLookupServiceImpl(enrollmentRepository);
    }

    @Test
    void findMostRecentEnrollmentForClassDefinitionReturnsSnapshot() {
        UUID studentUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();
        Enrollment enrollment = new Enrollment();
        enrollment.setUuid(UUID.randomUUID());
        enrollment.setStudentUuid(studentUuid);
        enrollment.setStatus(EnrollmentStatus.ATTENDED);
        enrollment.setCreatedDate(LocalDateTime.of(2026, 6, 22, 12, 0));
        enrollment.setLastModifiedDate(LocalDateTime.of(2026, 6, 22, 13, 0));

        when(enrollmentRepository.findLatestByStudentAndClassDefinitionUuid(studentUuid, classDefinitionUuid))
                .thenReturn(Optional.of(enrollment));

        Optional<ClassEnrollmentStatusSnapshot> result =
                service.findMostRecentEnrollmentForClassDefinition(studentUuid, classDefinitionUuid);

        assertThat(result).isPresent();
        assertThat(result.get().enrollmentUuid()).isEqualTo(enrollment.getUuid());
        assertThat(result.get().status()).isEqualTo("ATTENDED");
        assertThat(result.get().lastUpdatedAt()).isEqualTo(enrollment.getLastModifiedDate());
    }

    @Test
    void findMostRecentEnrollmentForClassDefinitionSkipsRepositoryForNullInput() {
        Optional<ClassEnrollmentStatusSnapshot> result =
                service.findMostRecentEnrollmentForClassDefinition(null, UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findMostRecentEnrollmentForClassDefinitionDelegatesToRepository() {
        UUID studentUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        when(enrollmentRepository.findLatestByStudentAndClassDefinitionUuid(studentUuid, classDefinitionUuid))
                .thenReturn(Optional.empty());

        Optional<ClassEnrollmentStatusSnapshot> result =
                service.findMostRecentEnrollmentForClassDefinition(studentUuid, classDefinitionUuid);

        assertThat(result).isEmpty();
        verify(enrollmentRepository).findLatestByStudentAndClassDefinitionUuid(studentUuid, classDefinitionUuid);
    }
}
