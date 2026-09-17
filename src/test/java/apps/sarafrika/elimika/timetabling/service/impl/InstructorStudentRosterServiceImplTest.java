package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import apps.sarafrika.elimika.timetabling.dto.InstructorStudentDTO;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorStudentRosterServiceImplTest {

    private static final UUID ORGANISATION = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private TrainingBranchLookupService trainingBranchLookupService;
    @Mock
    private DomainSecurityService domainSecurityService;

    private InstructorStudentRosterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InstructorStudentRosterServiceImpl(enrollmentRepository, trainingBranchLookupService,
                domainSecurityService);
    }

    @Test
    void aManagerOfAnotherOrganisationIsRefused() {
        UUID otherOrganisation = UUID.randomUUID();
        lenient().when(domainSecurityService.managesOrganisation(otherOrganisation)).thenReturn(true);
        when(domainSecurityService.managesOrganisation(ORGANISATION)).thenReturn(false);

        assertThatThrownBy(() -> service.listInstructorStudents(ORGANISATION, INSTRUCTOR, null, null, 0, 20))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(ORGANISATION.toString());
        verifyNoInteractions(enrollmentRepository, trainingBranchLookupService);
    }

    @Test
    void aCallerWhoManagesNothingIsRefused() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(domainSecurityService.managesOrganisation(ORGANISATION)).thenReturn(false);

        assertThatThrownBy(() -> service.listInstructorStudents(ORGANISATION, INSTRUCTOR, "amina", null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(enrollmentRepository, trainingBranchLookupService);
    }

    @Test
    void aPlatformAdminMayReadForSupport() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);
        when(enrollmentRepository.findInstructorStudentsForOrganisation(eq(ORGANISATION), eq(INSTRUCTOR), eq(null),
                eq("%"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertThat(service.listInstructorStudents(ORGANISATION, INSTRUCTOR, "  ", null, 0, 20).students()).isEmpty();
    }

    @Test
    void searchWildcardsAreTakenLiterallyAndThePageSizeIsCapped() {
        UUID classUuid = UUID.randomUUID();
        when(domainSecurityService.managesOrganisation(ORGANISATION)).thenReturn(true);
        when(enrollmentRepository.findInstructorStudentsForOrganisation(eq(ORGANISATION), eq(INSTRUCTOR),
                eq(classUuid), eq("%50\\%\\_off\\\\%"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.listInstructorStudents(ORGANISATION, INSTRUCTOR, " 50%_off\\ ", classUuid, -3, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(enrollmentRepository).findInstructorStudentsForOrganisation(eq(ORGANISATION), eq(INSTRUCTOR),
                eq(classUuid), eq("%50\\%\\_off\\\\%"), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(0, 100));
    }

    @Test
    void rowsAreMappedWithOneBatchedBranchLookupForThePage() {
        UUID branch = UUID.randomUUID();
        UUID classUuid = UUID.randomUUID();
        UUID amina = UUID.randomUUID();
        UUID brian = UUID.randomUUID();
        LocalDateTime enrolledAt = LocalDateTime.of(2031, 4, 1, 8, 0);
        when(domainSecurityService.managesOrganisation(ORGANISATION)).thenReturn(true);
        when(enrollmentRepository.findInstructorStudentsForOrganisation(eq(ORGANISATION), eq(INSTRUCTOR), eq(null),
                eq("%"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                row(amina, "Amina Otieno", classUuid, branch, Timestamp.valueOf(enrolledAt), 1, 2, "ENROLLED"),
                row(brian, "Brian Kamau", classUuid, branch, enrolledAt.atOffset(ZoneOffset.UTC), 0, 0, "WAITLISTED"))));
        when(enrollmentRepository.findInstructorClassOptionsForOrganisation(ORGANISATION, INSTRUCTOR))
                .thenReturn(List.<Object[]>of(new Object[]{classUuid, "Grade 5 Piano", "grade 5 piano"}));
        when(trainingBranchLookupService.findBranchNames(List.of(branch))).thenReturn(Map.of(branch, "Main Campus"));

        var roster = service.listInstructorStudents(ORGANISATION, INSTRUCTOR, null, null, 0, 20);

        InstructorStudentDTO first = roster.students().getContent().getFirst();
        assertThat(first.studentUuid()).isEqualTo(amina);
        assertThat(first.courseName()).isEqualTo("Beginner Piano");
        assertThat(first.sessionFormat()).isEqualTo(SessionFormat.GROUP);
        assertThat(first.locationType()).isEqualTo(LocationType.HYBRID);
        assertThat(first.scheduleSummary()).isEqualTo("Mon & Wed · 9:00–11:00");
        assertThat(first.branchName()).isEqualTo("Main Campus");
        assertThat(first.enrolledAt()).isEqualTo(enrolledAt);
        assertThat(first.attendanceRate()).isEqualTo(50.0);
        assertThat(first.enrollmentStatus()).isEqualTo(EnrollmentStatus.ENROLLED);

        InstructorStudentDTO second = roster.students().getContent().get(1);
        assertThat(second.enrolledAt()).isEqualTo(enrolledAt);
        assertThat(second.attendanceRate()).isNull();
        assertThat(second.enrollmentStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);

        assertThat(roster.classOptions()).singleElement()
                .satisfies(option -> assertThat(option.classTitle()).isEqualTo("Grade 5 Piano"));
        verify(trainingBranchLookupService).findBranchNames(List.of(branch));
    }

    private Object[] row(UUID student, String name, UUID classUuid, UUID branch, Object enrolledAt,
                         long attended, long recorded, String status) {
        return new Object[]{student, name, classUuid, "Grade 5 Piano", "Beginner Piano", "GROUP", "HYBRID", branch,
                enrolledAt, attended, recorded, status, "WEEKLY|MONDAY,WEDNESDAY|2031-05-05 09:00|11:00"};
    }
}
