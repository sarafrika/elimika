package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.InstructorRatingSummaryDTO;
import apps.sarafrika.elimika.instructor.dto.InstructorReviewDTO;
import java.util.List;
import java.util.UUID;

public interface InstructorReviewService {

    InstructorReviewDTO createReview(InstructorReviewDTO reviewDTO);

    List<InstructorReviewDTO> getReviewsForInstructor(UUID instructorUuid);

    /** Average rating and review count, aggregated in the database. */
    InstructorRatingSummaryDTO getRatingSummary(UUID instructorUuid);
}
