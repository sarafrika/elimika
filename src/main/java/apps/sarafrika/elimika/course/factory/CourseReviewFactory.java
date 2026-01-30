package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseReviewDTO;
import apps.sarafrika.elimika.course.model.CourseReview;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseReviewFactory {

    public static CourseReviewDTO toDTO(CourseReview entity) {
        if (entity == null) {
            return null;
        }
        return new CourseReviewDTO(
                entity.getUuid(),
                entity.getCourseUuid(),
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

    public static CourseReview toEntity(CourseReviewDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseReview entity = new CourseReview();
        entity.setUuid(dto.uuid());
        entity.setCourseUuid(dto.courseUuid());
        entity.setStudentUuid(dto.studentUuid());
        entity.setRating(dto.rating());
        entity.setHeadline(dto.headline());
        entity.setComments(dto.comments());
        entity.setIsAnonymous(dto.isAnonymous());
        return entity;
    }
}
