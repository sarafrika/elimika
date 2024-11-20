package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.CourseLearningObjective;

public record CourseLearningObjectiveResponseDTO(
        Long id,

        String objective
) {
    public static CourseLearningObjectiveResponseDTO from(CourseLearningObjective courseLearningObjective) {
        return new CourseLearningObjectiveResponseDTO(
                courseLearningObjective.getId(),
                courseLearningObjective.getObjective()
        );
    }
}
