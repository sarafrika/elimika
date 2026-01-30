package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseReviewDTO;

import java.util.List;
import java.util.UUID;

public interface CourseReviewService {

    CourseReviewDTO saveCourseReview(UUID courseUuid, CourseReviewDTO reviewDTO);

    List<CourseReviewDTO> getReviewsForCourse(UUID courseUuid);
}
