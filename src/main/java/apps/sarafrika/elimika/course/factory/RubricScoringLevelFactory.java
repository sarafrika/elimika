package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.RubricScoringLevelDTO;
import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory class for converting between RubricScoringLevel entities and DTOs
 * <p>
 * Provides static methods for bi-directional conversion between RubricScoringLevel
 * entity objects and their corresponding Data Transfer Objects (DTOs).
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RubricScoringLevelFactory {

    /**
     * Converts RubricScoringLevel entity to RubricScoringLevelDTO.
     *
     * @param rubricScoringLevel the entity to convert
     * @return converted DTO or null if input is null
     */
    public static RubricScoringLevelDTO toDTO(RubricScoringLevel rubricScoringLevel) {
        if (rubricScoringLevel == null) {
            return null;
        }
        return new RubricScoringLevelDTO(
                rubricScoringLevel.getUuid(),
                rubricScoringLevel.getRubricUuid(),
                rubricScoringLevel.getName(),
                rubricScoringLevel.getDescription(),
                rubricScoringLevel.getPoints(),
                rubricScoringLevel.getLevelOrder(),
                rubricScoringLevel.getColorCode(),
                rubricScoringLevel.getIsPassing(),
                rubricScoringLevel.getCreatedDate(),
                rubricScoringLevel.getCreatedBy(),
                rubricScoringLevel.getLastModifiedDate(),
                rubricScoringLevel.getLastModifiedBy()
        );
    }

    /**
     * Converts RubricScoringLevelDTO to RubricScoringLevel entity.
     *
     * @param dto the DTO to convert
     * @return converted entity or null if input is null
     */
    public static RubricScoringLevel toEntity(RubricScoringLevelDTO dto) {
        if (dto == null) {
            return null;
        }
        RubricScoringLevel rubricScoringLevel = new RubricScoringLevel();
        rubricScoringLevel.setUuid(dto.uuid());
        rubricScoringLevel.setRubricUuid(dto.rubricUuid());
        rubricScoringLevel.setName(dto.name());
        rubricScoringLevel.setDescription(dto.description());
        rubricScoringLevel.setPoints(dto.points());
        rubricScoringLevel.setLevelOrder(dto.levelOrder());
        rubricScoringLevel.setColorCode(dto.colorCode());
        rubricScoringLevel.setIsPassing(dto.isPassing());
        rubricScoringLevel.setCreatedDate(dto.createdDate());
        rubricScoringLevel.setCreatedBy(dto.createdBy());
        rubricScoringLevel.setLastModifiedDate(dto.updatedDate());
        rubricScoringLevel.setLastModifiedBy(dto.updatedBy());
        return rubricScoringLevel;
    }
}