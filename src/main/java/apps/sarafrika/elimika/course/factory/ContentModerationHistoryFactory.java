package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.ContentModerationHistoryDTO;
import apps.sarafrika.elimika.course.model.ContentModerationHistory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentModerationHistoryFactory {

    public static ContentModerationHistoryDTO toDTO(ContentModerationHistory entity) {
        if (entity == null) {
            return null;
        }
        return new ContentModerationHistoryDTO(
                entity.getUuid(),
                entity.getContentType(),
                entity.getContentUuid(),
                entity.getAction(),
                entity.getReason(),
                entity.getModeratorUuid(),
                entity.getCreatedDate(),
                entity.getCreatedBy()
        );
    }
}
