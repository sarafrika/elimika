package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.PrerequisiteType;

public record PrerequisiteTypeResponseDTO(Long id, String name) {

    public static PrerequisiteTypeResponseDTO from(PrerequisiteType prerequisiteType) {

        return new PrerequisiteTypeResponseDTO(prerequisiteType.getId(), prerequisiteType.getName());
    }
}
