package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.ContentProgressDTO;
import apps.sarafrika.elimika.course.model.ContentProgress;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentProgressFactory {

    // Convert ContentProgress entity to ContentProgressDTO
    public static ContentProgressDTO toDTO(ContentProgress contentProgress) {
        if (contentProgress == null) {
            return null;
        }
        return new ContentProgressDTO(
                contentProgress.getUuid(),
                contentProgress.getEnrollmentUuid(),
                contentProgress.getContentUuid(),
                contentProgress.getIsAccessed(),
                contentProgress.getIsCompleted(),
                contentProgress.getAccessCount(),
                contentProgress.getFirstAccessedAt(),
                contentProgress.getLastAccessedAt(),
                contentProgress.getCompletedAt(),
                contentProgress.getCreatedDate(),
                contentProgress.getCreatedBy(),
                contentProgress.getLastModifiedDate(),
                contentProgress.getLastModifiedBy()
        );
    }

    // Convert ContentProgressDTO to ContentProgress entity
    public static ContentProgress toEntity(ContentProgressDTO dto) {
        if (dto == null) {
            return null;
        }
        ContentProgress contentProgress = new ContentProgress();
        contentProgress.setUuid(dto.uuid());
        contentProgress.setEnrollmentUuid(dto.enrollmentUuid());
        contentProgress.setContentUuid(dto.contentUuid());
        contentProgress.setIsAccessed(dto.isAccessed());
        contentProgress.setIsCompleted(dto.isCompleted());
        contentProgress.setAccessCount(dto.accessCount());
        contentProgress.setFirstAccessedAt(dto.firstAccessedAt());
        contentProgress.setLastAccessedAt(dto.lastAccessedAt());
        contentProgress.setCompletedAt(dto.completedAt());
        contentProgress.setCreatedDate(dto.createdDate());
        contentProgress.setCreatedBy(dto.createdBy());
        contentProgress.setLastModifiedDate(dto.updatedDate());
        contentProgress.setLastModifiedBy(dto.updatedBy());
        return contentProgress;
    }
}