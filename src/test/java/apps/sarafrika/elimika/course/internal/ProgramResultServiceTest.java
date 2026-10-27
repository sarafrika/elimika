package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgramResultServiceTest {

    @Mock
    private ProgramCourseRepository programCourseRepository;
    @Mock
    private ProgramEnrollmentRepository programEnrollmentRepository;
    @Mock
    private TrainingProgramRepository programRepository;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @InjectMocks
    private ProgramResultService service;

    private final UUID programUuid = UUID.randomUUID();
    private final UUID studentUuid = UUID.randomUUID();
    private final UUID courseUuid = UUID.randomUUID();
    private ProgramEnrollment programEnrollment;
    private CourseEnrollment courseEnrollment;

    @BeforeEach
    void setUp() {
        programEnrollment = new ProgramEnrollment();
        programEnrollment.setProgramUuid(programUuid);
        programEnrollment.setStudentUuid(studentUuid);
        programEnrollment.setStatus(EnrollmentStatus.ACTIVE);
        ProgramCourse member = new ProgramCourse();
        member.setProgramUuid(programUuid);
        member.setCourseUuid(courseUuid);
        member.setIsRequired(true);
        when(programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid)).thenReturn(List.of(member));
        courseEnrollment = new CourseEnrollment();
        courseEnrollment.setUuid(UUID.randomUUID());
        courseEnrollment.setCourseUuid(courseUuid);
        when(courseEnrollmentRepository.findByStudentUuidAndCourseUuid(studentUuid, courseUuid))
                .thenReturn(Optional.of(courseEnrollment));

        courseEnrollment.setFinalGrade(new BigDecimal("78"));
    }

    @Test
    void averagesCourseFinalsAndPassesAtTheMark() {
        courseEnrollment.setResultStatus(CourseResultStatus.PASSED);
        passMark("78");

        service.recompute(programEnrollment);

        assertThat(programEnrollment.getFinalGrade()).isEqualByComparingTo("78.00");
        assertThat(programEnrollment.getResultStatus()).isEqualTo(CourseResultStatus.PASSED);
        assertThat(programEnrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(programEnrollment.getProgressPercentage()).isEqualByComparingTo("100");
    }

    @Test
    void failsBelowTheMarkAndWaitsWhileACourseIsUndecided() {
        courseEnrollment.setResultStatus(CourseResultStatus.IN_PROGRESS);
        passMark("80");
        service.recompute(programEnrollment);
        assertThat(programEnrollment.getResultStatus()).isEqualTo(CourseResultStatus.IN_PROGRESS);

        courseEnrollment.setResultStatus(CourseResultStatus.PASSED);
        service.recompute(programEnrollment);
        assertThat(programEnrollment.getResultStatus()).isEqualTo(CourseResultStatus.FAILED);
        assertThat(programEnrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    }

    private void passMark(String passMark) {
        TrainingProgram program = new TrainingProgram();
        program.setUuid(programUuid);
        program.setPassMark(new BigDecimal(passMark));
        when(programRepository.findByUuid(programUuid)).thenReturn(Optional.of(program));
    }
}
