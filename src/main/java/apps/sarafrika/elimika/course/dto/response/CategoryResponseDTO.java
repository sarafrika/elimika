package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.Category;

public record CategoryResponseDTO(
        Long id,

        String name,

        String description
) {
    public static CategoryResponseDTO from(Category category) {

        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
