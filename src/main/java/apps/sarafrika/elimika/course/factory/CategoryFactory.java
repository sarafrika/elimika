package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.model.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryFactory {

    // Convert Category entity to CategoryDTO
    public static CategoryDTO toDTO(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDTO(
                category.getUuid(),
                category.getName(),
                category.getDescription(),
                category.getParentUuid(),
                category.getIsActive(),
                category.getCreatedDate(),
                category.getCreatedBy(),
                category.getLastModifiedDate(),
                category.getLastModifiedBy()
        );
    }

    // Convert CategoryDTO to Category entity
    public static Category toEntity(CategoryDTO dto) {
        if (dto == null) {
            return null;
        }
        Category category = new Category();
        category.setUuid(dto.uuid());
        category.setName(dto.name());
        category.setDescription(dto.description());
        category.setParentUuid(dto.parentUuid());
        category.setIsActive(dto.isActive());
        category.setCreatedDate(dto.createdDate());
        category.setCreatedBy(dto.createdBy());
        category.setLastModifiedDate(dto.updatedDate());
        category.setLastModifiedBy(dto.updatedBy());
        return category;
    }
}
