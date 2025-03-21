package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.CategoryDTO;
import apps.sarafrika.elimika.course.model.Category;

/**
 * Utility class for mapping between Category entities and DTOs
 */
public class CategoryMapper {

    private CategoryMapper() {}

    /**
     * Converts a Category entity to a CategoryDTO
     *
     * @param entity the Category entity to convert
     * @return the corresponding CategoryDTO
     */
    public static CategoryDTO toDto(Category entity) {
        if (entity == null) {
            return null;
        }

        return new CategoryDTO(
                entity.getUuid(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy(),
                entity.getCreatedDate(),
                entity.getLastModifiedDate()
        );
    }

    /**
     * Converts a CategoryDTO to a Category entity
     *
     * @param dto the CategoryDTO to convert
     * @return the corresponding Category entity
     */
    public static Category toEntity(CategoryDTO dto) {
        if (dto == null) {
            return null;
        }

        Category category = new Category();
        category.setUuid(dto.uuid());
        category.setName(dto.name());
        category.setDescription(dto.description());

        return category;
    }
}