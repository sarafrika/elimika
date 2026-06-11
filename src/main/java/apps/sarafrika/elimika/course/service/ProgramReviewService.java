package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ProgramRatingSummaryDTO;
import apps.sarafrika.elimika.course.dto.ProgramReviewDTO;
import apps.sarafrika.elimika.course.dto.ProgramReviewRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProgramReviewService {

    ProgramReviewDTO saveProgramReview(UUID programUuid, ProgramReviewRequest reviewRequest);

    Page<ProgramReviewDTO> getReviewsForProgram(UUID programUuid, Pageable pageable);

    ProgramRatingSummaryDTO getRatingSummary(UUID programUuid);
}
