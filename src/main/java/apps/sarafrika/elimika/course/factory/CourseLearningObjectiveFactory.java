package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.request.UpdateCourseLearningObjectiveRequestDTO;
import apps.sarafrika.elimika.course.model.CourseLearningObjective;

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
