package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.ProgramReviewDTO;
import apps.sarafrika.elimika.course.model.ProgramReview;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProgramReviewFactory {

    public static ProgramReviewDTO toDTO(ProgramReview entity) {
        if (entity == null) {
            return null;
        }
        return new ProgramReviewDTO(
                entity.getUuid(),
                entity.getProgramUuid(),
                entity.getStudentUuid(),
                entity.getRating(),
                entity.getHeadline(),
                entity.getComments(),
                entity.getIsAnonymous(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }
}
