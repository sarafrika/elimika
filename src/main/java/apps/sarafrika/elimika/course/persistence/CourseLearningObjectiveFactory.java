package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.CreateCourseLearningObjectiveRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseLearningObjectiveRequestDTO;

public class CourseLearningObjectiveFactory {

    public static CourseLearningObjective create(CreateCourseLearningObjectiveRequestDTO createCourseLearningObjectiveRequestDTO) {

        return CourseLearningObjective.builder()
                .objective(createCourseLearningObjectiveRequestDTO.objective())
                .build();
    }

    public static void update(CourseLearningObjective courseLearningObjective, UpdateCourseLearningObjectiveRequestDTO updateCourseLearningObjectiveRequestDTO) {

        courseLearningObjective.setObjective(updateCourseLearningObjectiveRequestDTO.objective());
    }
}
