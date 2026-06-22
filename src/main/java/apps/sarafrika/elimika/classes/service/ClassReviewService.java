package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassRatingSummaryDTO;
import apps.sarafrika.elimika.classes.dto.ClassReviewDTO;
import apps.sarafrika.elimika.classes.dto.ClassReviewRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ClassReviewService {

    ClassReviewDTO saveClassReview(UUID classDefinitionUuid, ClassReviewRequest reviewRequest);

    Page<ClassReviewDTO> getReviewsForClass(UUID classDefinitionUuid, Pageable pageable);

    ClassRatingSummaryDTO getRatingSummary(UUID classDefinitionUuid);
}
