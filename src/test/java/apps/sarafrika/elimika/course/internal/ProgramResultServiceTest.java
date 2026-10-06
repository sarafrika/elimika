package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentScore;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.ProgramAssessment;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.ProgramAssessmentRepository;
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
    private ProgramAssessmentRepository programAssessmentRepository;
    @Mock
    private TrainingProgramRepository programRepository;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;
    @Mock
    private CourseAssessmentScoreRepository courseAssessmentScoreRepository;

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

        // Practical 40 scored 90, Quiz 60 scored 70: (90 x 40 + 70 x 60) / 100 = 78.
        component("Practical", "40", "90");
        component("Quiz", "60", "70");
    }

    @Test
    void weightsCourseScoresByProgramComponentAndPassesAtTheMark() {
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

    private void component(String title, String weight, String score) {
        ProgramAssessment component = new ProgramAssessment();
        component.setUuid(UUID.randomUUID());
        component.setProgramUuid(programUuid);
        component.setTitle(title);
        component.setWeightPercentage(new BigDecimal(weight));
        CourseAssessment linked = new CourseAssessment();
        linked.setUuid(UUID.randomUUID());
        linked.setCourseUuid(courseUuid);
        CourseAssessmentScore courseScore = new CourseAssessmentScore();
        courseScore.setPercentage(new BigDecimal(score));
        List<ProgramAssessment> existing = new java.util.ArrayList<>(
                programAssessmentRepository.findByProgramUuidOrderByCreatedDateAsc(programUuid));
        existing.add(component);
        when(programAssessmentRepository.findByProgramUuidOrderByCreatedDateAsc(programUuid)).thenReturn(existing);
        when(courseAssessmentRepository.findByProgramAssessmentUuidAndActiveTrue(component.getUuid())).thenReturn(List.of(linked));
        when(courseAssessmentScoreRepository.findByEnrollmentUuidAndAssessmentUuid(courseEnrollment.getUuid(), linked.getUuid()))
                .thenReturn(Optional.of(courseScore));
    }

    private void passMark(String passMark) {
        TrainingProgram program = new TrainingProgram();
        program.setUuid(programUuid);
        program.setPassMark(new BigDecimal(passMark));
        when(programRepository.findByUuid(programUuid)).thenReturn(Optional.of(program));
    }
}
