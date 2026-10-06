package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Active assessment weights must total exactly 100% before a course goes live; edits in between may be partial. */
@Component
@RequiredArgsConstructor
public class CourseAssessmentWeightRule {

    private static final BigDecimal FULL_WEIGHT = new BigDecimal("100");

    private final CourseAssessmentRepository assessmentRepository;

    public void enforce(UUID courseUuid) {
        List<CourseAssessment> active = assessmentRepository.findByCourseUuidOrderByCreatedDateAsc(courseUuid).stream()
                .filter(assessment -> !Boolean.FALSE.equals(assessment.getActive()))
                .toList();
        if (active.isEmpty()) {
            return;
        }
        BigDecimal total = active.stream()
                .map(assessment -> assessment.getWeightPercentage() == null ? BigDecimal.ZERO : assessment.getWeightPercentage())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(FULL_WEIGHT) != 0) {
            throw new IllegalStateException("Assessment weights must add up to 100% before the course goes live; they add up to "
                    + total.stripTrailingZeros().toPlainString() + "%.");
        }
    }
}
