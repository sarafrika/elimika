package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.dto.InstructorReviewDTO;
import apps.sarafrika.elimika.instructor.model.InstructorReview;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorReviewFactory {

    public static InstructorReviewDTO toDTO(InstructorReview entity) {
        if (entity == null) {
            return null;
        }
        return new InstructorReviewDTO(
                entity.getUuid(),
                entity.getInstructorUuid(),
                entity.getStudentUuid(),
                entity.getEnrollmentUuid(),
                entity.getRating(),
                entity.getHeadline(),
                entity.getComments(),
                entity.getClarityRating(),
                entity.getEngagementRating(),
                entity.getPunctualityRating(),
                entity.getIsAnonymous(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }

    public static InstructorReview toEntity(InstructorReviewDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorReview entity = new InstructorReview();
        entity.setUuid(dto.uuid());
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setStudentUuid(dto.studentUuid());
        entity.setEnrollmentUuid(dto.enrollmentUuid());
        entity.setRating(dto.rating());
        entity.setHeadline(dto.headline());
        entity.setComments(dto.comments());
        entity.setClarityRating(dto.clarityRating());
        entity.setEngagementRating(dto.engagementRating());
        entity.setPunctualityRating(dto.punctualityRating());
        entity.setIsAnonymous(dto.isAnonymous());
        return entity;
    }
}
