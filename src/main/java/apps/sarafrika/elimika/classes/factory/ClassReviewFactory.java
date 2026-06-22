package apps.sarafrika.elimika.classes.factory;

import apps.sarafrika.elimika.classes.dto.ClassReviewDTO;
import apps.sarafrika.elimika.classes.model.ClassReview;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassReviewFactory {

    public static ClassReviewDTO toDTO(ClassReview entity) {
        if (entity == null) {
            return null;
        }
        return new ClassReviewDTO(
                entity.getUuid(),
                entity.getClassDefinitionUuid(),
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
