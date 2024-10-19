package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.CreateLearningMaterialRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLearningMaterialRequestDTO;

public class LearningMaterialFactory {

    public static LearningMaterial create(CreateLearningMaterialRequestDTO createLearningMaterialRequestDTO) {

        return LearningMaterial.builder()
                .title(createLearningMaterialRequestDTO.title())
                .type(createLearningMaterialRequestDTO.type())
                .url(createLearningMaterialRequestDTO.url())
                .courseId(createLearningMaterialRequestDTO.courseId())
                .lessonId(createLearningMaterialRequestDTO.lessonId())
                .build();
    }

    public static void update(LearningMaterial learningMaterial, UpdateLearningMaterialRequestDTO updateLearningMaterialRequestDTO) {

        learningMaterial.setTitle(updateLearningMaterialRequestDTO.title());
        learningMaterial.setType(updateLearningMaterialRequestDTO.type());
        learningMaterial.setUrl(updateLearningMaterialRequestDTO.url());
    }
}
