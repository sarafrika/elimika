package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.DifficultyLevelDTO;
import apps.sarafrika.elimika.course.model.DifficultyLevel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DifficultyLevelFactory {

    // Convert DifficultyLevel entity to DifficultyLevelDTO
    public static DifficultyLevelDTO toDTO(DifficultyLevel difficultyLevel) {
        if (difficultyLevel == null) {
            return null;
        }
        return new DifficultyLevelDTO(
                difficultyLevel.getUuid(),
                difficultyLevel.getName(),
                difficultyLevel.getLevelOrder(),
                difficultyLevel.getDescription(),
                difficultyLevel.getCreatedDate(),
                difficultyLevel.getCreatedBy(),
                difficultyLevel.getLastModifiedDate(),
                difficultyLevel.getLastModifiedBy()
        );
    }

    // Convert DifficultyLevelDTO to DifficultyLevel entity
    public static DifficultyLevel toEntity(DifficultyLevelDTO dto) {
        if (dto == null) {
            return null;
        }
        DifficultyLevel difficultyLevel = new DifficultyLevel();
        difficultyLevel.setUuid(dto.uuid());
        difficultyLevel.setName(dto.name());
        difficultyLevel.setLevelOrder(dto.levelOrder());
        difficultyLevel.setDescription(dto.description());
        difficultyLevel.setCreatedDate(dto.createdDate());
        difficultyLevel.setCreatedBy(dto.createdBy());
        difficultyLevel.setLastModifiedDate(dto.updatedDate());
        difficultyLevel.setLastModifiedBy(dto.updatedBy());
        return difficultyLevel;
    }
}