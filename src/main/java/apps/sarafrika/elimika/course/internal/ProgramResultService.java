package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
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

/** Recomputes a learner's programme mark and result whenever a programme course is regraded or reweighted. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProgramResultService {

    private static final int SCALE = 2;

    private final ProgramCourseRepository programCourseRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final TrainingProgramRepository programRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    @EventListener
    public void onCourseGradeRecalculated(CourseGradeRecalculatedEvent event) {
        for (ProgramCourse membership : programCourseRepository.findByCourseUuid(event.courseUuid())) {
            programEnrollmentRepository.findFirstByStudentUuidAndProgramUuidOrderByCreatedDateDesc(
                            event.studentUuid(), membership.getProgramUuid())
                    .ifPresent(this::recompute);
        }
    }

    @EventListener
    public void onProgramWeightsChanged(ProgramWeightsChangedEvent event) {
        programEnrollmentRepository.findByProgramUuid(event.programUuid()).forEach(this::recompute);
    }

    public void recompute(ProgramEnrollment enrollment) {
        List<ProgramCourse> members = programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(enrollment.getProgramUuid());
        Map<UUID, CourseEnrollment> courseEnrollments = new HashMap<>();
        for (ProgramCourse member : members) {
            courseEnrollmentRepository.findByStudentUuidAndCourseUuid(enrollment.getStudentUuid(), member.getCourseUuid())
                    .ifPresent(courseEnrollment -> courseEnrollments.put(member.getCourseUuid(), courseEnrollment));
        }

        List<ProgramCourse> required = members.stream().filter(member -> !Boolean.FALSE.equals(member.getIsRequired())).toList();
        List<ProgramCourse> decided = required.stream().filter(member -> isDecided(courseEnrollments, member)).toList();
        enrollment.setFinalGrade(weightedFinal(required, decided, courseEnrollments));
        enrollment.setFinalGradeProvisional(required.isEmpty() || decided.size() < required.size());
        long passed = required.stream().filter(member -> result(courseEnrollments, member) == CourseResultStatus.PASSED).count();
        enrollment.setProgressPercentage(required.isEmpty() ? null
                : BigDecimal.valueOf(passed * 100).divide(BigDecimal.valueOf(required.size()), SCALE, RoundingMode.HALF_UP));
        applyResult(enrollment, programResult(enrollment, required, decided, courseEnrollments));
        programEnrollmentRepository.save(enrollment);
    }

    /**
     * Weighted mean of decided mandatory course finals, re-normalised over the decided weight so undecided
     * courses are left out rather than counted as zero. Unweighted programmes weigh their courses equally.
     */
    private BigDecimal weightedFinal(List<ProgramCourse> required, List<ProgramCourse> decided,
                                     Map<UUID, CourseEnrollment> courseEnrollments) {
        boolean weighted = !required.isEmpty() && required.stream().allMatch(member -> member.getWeightPercentage() != null);
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal weights = BigDecimal.ZERO;
        for (ProgramCourse member : decided) {
            BigDecimal weight = weighted ? member.getWeightPercentage() : BigDecimal.ONE;
            sum = sum.add(courseEnrollments.get(member.getCourseUuid()).getFinalGrade().multiply(weight));
            weights = weights.add(weight);
        }
        return weights.signum() == 0 ? null : sum.divide(weights, SCALE, RoundingMode.HALF_UP);
    }

    /** Passes once every mandatory course is decided and passed and the mark reaches the programme pass mark. */
    private CourseResultStatus programResult(ProgramEnrollment enrollment, List<ProgramCourse> required,
                                             List<ProgramCourse> decided, Map<UUID, CourseEnrollment> courseEnrollments) {
        if (required.isEmpty() || decided.size() < required.size()) {
            return CourseResultStatus.IN_PROGRESS;
        }
        boolean allPassed = required.stream().allMatch(member -> result(courseEnrollments, member) == CourseResultStatus.PASSED);
        BigDecimal passMark = programRepository.findByUuid(enrollment.getProgramUuid())
                .map(program -> program.getPassMark()).orElse(null);
        BigDecimal finalGrade = enrollment.getFinalGrade();
        boolean markReached = passMark == null || (finalGrade != null && finalGrade.compareTo(passMark) >= 0);
        return allPassed && markReached ? CourseResultStatus.PASSED : CourseResultStatus.FAILED;
    }

    private static boolean isDecided(Map<UUID, CourseEnrollment> courseEnrollments, ProgramCourse member) {
        CourseEnrollment courseEnrollment = courseEnrollments.get(member.getCourseUuid());
        if (courseEnrollment == null || courseEnrollment.getFinalGrade() == null) {
            return false;
        }
        CourseResultStatus result = courseEnrollment.getResultStatus();
        return result == CourseResultStatus.PASSED || result == CourseResultStatus.FAILED;
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
}
