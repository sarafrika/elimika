package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseVersionSnapshotDTO;
import apps.sarafrika.elimika.course.model.CourseVersionSnapshot;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseVersionSnapshotFactory {

    public static CourseVersionSnapshotDTO toDTO(CourseVersionSnapshot entity) {
        if (entity == null) {
            return null;
        }
        return new CourseVersionSnapshotDTO(
                entity.getUuid(),
                entity.getCourseUuid(),
                entity.getVersionNumber(),
                entity.getSnapshot(),
                entity.getPendingEditUuid(),
                entity.getCreatedDate(),
                entity.getCreatedBy()
        );
    }
}
