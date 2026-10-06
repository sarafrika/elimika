package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentScore;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.ProgramAssessment;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.ProgramAssessmentRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Recomputes a learner's program grade and result whenever one of the program's courses is regraded. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProgramResultService {

    private static final int SCALE = 2;

    private final ProgramCourseRepository programCourseRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final ProgramAssessmentRepository programAssessmentRepository;
    private final TrainingProgramRepository programRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final CourseAssessmentScoreRepository courseAssessmentScoreRepository;

    @EventListener
    public void onCourseGradeRecalculated(CourseGradeRecalculatedEvent event) {
        for (ProgramCourse membership : programCourseRepository.findByCourseUuid(event.courseUuid())) {
            programEnrollmentRepository.findFirstByStudentUuidAndProgramUuidOrderByCreatedDateDesc(
                            event.studentUuid(), membership.getProgramUuid())
                    .ifPresent(this::recompute);
        }
    }

    public void recompute(ProgramEnrollment enrollment) {
        List<ProgramCourse> members = programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(enrollment.getProgramUuid());
        Map<UUID, CourseEnrollment> courseEnrollments = new HashMap<>();
        for (ProgramCourse member : members) {
            courseEnrollmentRepository.findByStudentUuidAndCourseUuid(enrollment.getStudentUuid(), member.getCourseUuid())
                    .ifPresent(courseEnrollment -> courseEnrollments.put(member.getCourseUuid(), courseEnrollment));
        }

        enrollment.setFinalGrade(finalGrade(enrollment.getProgramUuid(), courseEnrollments));
        List<ProgramCourse> required = members.stream().filter(member -> !Boolean.FALSE.equals(member.getIsRequired())).toList();
        long passed = required.stream().filter(member -> result(courseEnrollments, member) == CourseResultStatus.PASSED).count();
        enrollment.setProgressPercentage(required.isEmpty() ? null
                : BigDecimal.valueOf(passed * 100).divide(BigDecimal.valueOf(required.size()), SCALE, RoundingMode.HALF_UP));
        applyResult(enrollment, programResult(enrollment, required, courseEnrollments));
        programEnrollmentRepository.save(enrollment);
    }

    /** Program components average their linked course scores; without components, course final grades are averaged. */
    private BigDecimal finalGrade(UUID programUuid, Map<UUID, CourseEnrollment> courseEnrollments) {
        List<ProgramAssessment> components = programAssessmentRepository.findByProgramUuidOrderByCreatedDateAsc(programUuid)
                .stream().filter(component -> !Boolean.FALSE.equals(component.getActive())).toList();
        if (components.isEmpty()) {
            return average(courseEnrollments.values().stream().map(CourseEnrollment::getFinalGrade).toList());
        }
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weights = BigDecimal.ZERO;
        for (ProgramAssessment component : components) {
            BigDecimal score = average(courseAssessmentRepository.findByProgramAssessmentUuidAndActiveTrue(component.getUuid())
                    .stream()
                    .map(linked -> linkedScore(linked, courseEnrollments))
                    .toList());
            if (score != null) {
                weighted = weighted.add(score.multiply(component.getWeightPercentage()));
                weights = weights.add(component.getWeightPercentage());
            }
        }
        return weights.signum() == 0 ? null : weighted.divide(weights, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal linkedScore(CourseAssessment linked, Map<UUID, CourseEnrollment> courseEnrollments) {
        CourseEnrollment courseEnrollment = courseEnrollments.get(linked.getCourseUuid());
        if (courseEnrollment == null) {
            return null;
        }
        return courseAssessmentScoreRepository.findByEnrollmentUuidAndAssessmentUuid(courseEnrollment.getUuid(), linked.getUuid())
                .map(CourseAssessmentScore::getPercentage)
                .orElse(null);
    }

    private CourseResultStatus programResult(ProgramEnrollment enrollment, List<ProgramCourse> required,
                                             Map<UUID, CourseEnrollment> courseEnrollments) {
        if (required.isEmpty() || required.stream().anyMatch(member -> {
            CourseResultStatus result = result(courseEnrollments, member);
            return result == null || result == CourseResultStatus.IN_PROGRESS;
        })) {
            return CourseResultStatus.IN_PROGRESS;
        }
        BigDecimal passMark = programRepository.findByUuid(enrollment.getProgramUuid())
                .map(program -> program.getPassMark()).orElse(null);
        if (passMark == null) {
            boolean allPassed = required.stream().allMatch(member -> result(courseEnrollments, member) == CourseResultStatus.PASSED);
            return allPassed ? CourseResultStatus.PASSED : CourseResultStatus.FAILED;
        }
        BigDecimal finalGrade = enrollment.getFinalGrade();
        return finalGrade != null && finalGrade.compareTo(passMark) >= 0 ? CourseResultStatus.PASSED : CourseResultStatus.FAILED;
    }

    private void applyResult(ProgramEnrollment enrollment, CourseResultStatus result) {
        if (result == enrollment.getResultStatus()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        enrollment.setResultStatus(result);
        enrollment.setResultDecidedAt(result == CourseResultStatus.IN_PROGRESS ? null : now);
        if (result == CourseResultStatus.PASSED) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            if (enrollment.getCompletionDate() == null) {
                enrollment.setCompletionDate(now);
            }
        } else if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setCompletionDate(null);
        }
        log.info("Program enrolment {} result is now {}", enrollment.getUuid(), result);
    }

    private static CourseResultStatus result(Map<UUID, CourseEnrollment> courseEnrollments, ProgramCourse member) {
        return Optional.ofNullable(courseEnrollments.get(member.getCourseUuid()))
                .map(CourseEnrollment::getResultStatus).orElse(null);
    }

    private static BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream().filter(java.util.Objects::nonNull).toList();
        if (present.isEmpty()) {
            return null;
        }
        return present.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(present.size()), SCALE, RoundingMode.HALF_UP);
    }
}
