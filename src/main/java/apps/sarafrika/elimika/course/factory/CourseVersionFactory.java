package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseVersionDTO;
import apps.sarafrika.elimika.course.model.CourseVersion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseVersionFactory {

    public static CourseVersionDTO toDTO(CourseVersion entity) {
        if (entity == null) {
            return null;
        }

        return new CourseVersionDTO(
                entity.getUuid(),
                entity.getCourseUuid(),
                entity.getVersionNumber(),
                entity.getSnapshotHash(),
                entity.getSnapshotPayloadJson(),
                entity.getPublishedAt(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }
}
