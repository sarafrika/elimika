package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.GradingLevelDTO;
import apps.sarafrika.elimika.course.model.GradingLevel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GradingLevelFactory {

    // Convert GradingLevel entity to GradingLevelDTO
    public static GradingLevelDTO toDTO(GradingLevel gradingLevel) {
        if (gradingLevel == null) {
            return null;
        }
        return new GradingLevelDTO(
                gradingLevel.getUuid(),
                gradingLevel.getName(),
                gradingLevel.getPoints(),
                gradingLevel.getLevelOrder(),
                gradingLevel.getCreatedDate(),
                gradingLevel.getCreatedBy(),
                gradingLevel.getLastModifiedDate(),
                gradingLevel.getLastModifiedBy()
        );
    }

    // Convert GradingLevelDTO to GradingLevel entity
    public static GradingLevel toEntity(GradingLevelDTO dto) {
        if (dto == null) {
            return null;
        }
        GradingLevel gradingLevel = new GradingLevel();
        gradingLevel.setUuid(dto.uuid());
        gradingLevel.setName(dto.name());
        gradingLevel.setPoints(dto.points());
        gradingLevel.setLevelOrder(dto.levelOrder());
        gradingLevel.setCreatedDate(dto.createdDate());
        gradingLevel.setCreatedBy(dto.createdBy());
        gradingLevel.setLastModifiedDate(dto.updatedDate());
        gradingLevel.setLastModifiedBy(dto.updatedBy());
        return gradingLevel;
    }
}