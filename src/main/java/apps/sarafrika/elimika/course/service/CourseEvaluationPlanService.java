package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseEvaluationPlanCellRequest;
import apps.sarafrika.elimika.course.dto.CourseEvaluationPlanDTO;

import java.util.List;
import java.util.UUID;

/** The lesson x component grid: which lessons each per-lesson component grades, and with which rubric. */
public interface CourseEvaluationPlanService {

    CourseEvaluationPlanDTO getPlan(UUID courseUuid);

    CourseEvaluationPlanDTO updatePlan(UUID courseUuid, List<CourseEvaluationPlanCellRequest> cells);
}
