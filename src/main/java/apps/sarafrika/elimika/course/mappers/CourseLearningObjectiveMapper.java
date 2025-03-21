package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.CourseLearningObjectiveDTO;
import apps.sarafrika.elimika.course.model.CourseLearningObjective;

/**
 * Utility class for mapping between CourseLearningObjective entities and DTOs
 */
public class CourseLearningObjectiveMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private CourseLearningObjectiveMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Converts a CourseLearningObjective entity to a CourseLearningObjectiveDTO
     *
     * @param entity the CourseLearningObjective entity to convert
     * @return the corresponding CourseLearningObjectiveDTO
     */
    public static CourseLearningObjectiveDTO toDto(CourseLearningObjective entity) {
        if (entity == null) {
            return null;
        }

        return CourseLearningObjectiveDTO.builder()
                .uuid(entity.getUuid())
                .courseUuid(entity.getCourseUuid())
                .objective(entity.getObjective())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Converts a CourseLearningObjectiveDTO to a CourseLearningObjective entity
     *
     * @param dto the CourseLearningObjectiveDTO to convert
     * @return the corresponding CourseLearningObjective entity
     */
    public static CourseLearningObjective toEntity(CourseLearningObjectiveDTO dto) {
        if (dto == null) {
            return null;
        }

        CourseLearningObjective objective = new CourseLearningObjective();
        objective.setUuid(dto.uuid());
        objective.setCourseUuid(dto.courseUuid());
        objective.setObjective(dto.objective());

        return objective;
    }
}