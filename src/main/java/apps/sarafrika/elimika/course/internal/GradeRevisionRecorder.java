package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseAssessmentGradeRevision;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemScore;
import apps.sarafrika.elimika.course.repository.CourseAssessmentGradeRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Appends one history row for every grade saved, so earlier attempts and regrades stay visible. */
@Component
@RequiredArgsConstructor
public class GradeRevisionRecorder {

    private final CourseAssessmentGradeRevisionRepository revisionRepository;

    public CourseAssessmentGradeRevision record(CourseAssessmentLineItem lineItem, CourseAssessmentLineItemScore saved,
                                                List<CourseAssessmentGradeRevision.Criterion> criteria) {
        CourseAssessmentGradeRevision revision = new CourseAssessmentGradeRevision();
        revision.setLineItemUuid(saved.getLineItemUuid());
        revision.setEnrollmentUuid(saved.getEnrollmentUuid());
        revision.setRevisionNumber(revisionRepository.findLatestRevisionNumber(
                saved.getLineItemUuid(), saved.getEnrollmentUuid()) + 1);
        revision.setItemType(lineItem.getItemType());
        revision.setScore(saved.getScore());
        revision.setMaxScore(saved.getMaxScore());
        revision.setPercentage(saved.getPercentage());
        revision.setComments(saved.getComments());
        revision.setCriteria(criteria);
        revision.setGradedAt(saved.getGradedAt());
        revision.setGradedByUuid(saved.getGradedByUuid());
        return revisionRepository.save(revision);
    }
}
