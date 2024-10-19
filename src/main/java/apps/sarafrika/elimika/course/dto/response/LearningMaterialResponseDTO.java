package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.LearningMaterial;

public record LearningMaterialResponseDTO(
        Long id,

        String title,

        String type,

        String url
) {

    public static LearningMaterialResponseDTO from(LearningMaterial learningMaterial) {

        return new LearningMaterialResponseDTO(
                learningMaterial.getId(),
                learningMaterial.getTitle(),
                learningMaterial.getType(),
                learningMaterial.getUrl()
        );
    }
}
