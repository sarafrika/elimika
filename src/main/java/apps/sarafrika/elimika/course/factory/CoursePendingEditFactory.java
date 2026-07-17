package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CoursePendingEditDTO;
import apps.sarafrika.elimika.course.model.CoursePendingEdit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CoursePendingEditFactory {

    public static CoursePendingEditDTO toDTO(CoursePendingEdit entity) {
        if (entity == null) {
            return null;
        }
        return new CoursePendingEditDTO(
                entity.getUuid(),
                entity.getCourseUuid(),
                entity.getDraftCourseUuid(),
                entity.getStatus(),
                entity.getSubmittedByUuid(),
                entity.getSubmittedAt(),
                entity.getReviewedByUuid(),
                entity.getReviewedAt(),
                entity.getReviewReason()
        );
    }
}
