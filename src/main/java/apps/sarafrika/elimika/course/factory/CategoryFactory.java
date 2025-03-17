package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.request.UpdateCategoryRequestDTO;
import apps.sarafrika.elimika.course.model.Category;

public class CategoryFactory {

    public static Category create(CreateCategoryRequestDTO createCategoryRequestDTO) {

        return Category.builder()
                .name(createCategoryRequestDTO.name().trim().toLowerCase())
                .description(createCategoryRequestDTO.description() != null ? createCategoryRequestDTO.description().trim() : null)
                .build();
    }

    public static void update(Category category, UpdateCategoryRequestDTO updateCategoryRequestDTO) {

        category.setName(updateCategoryRequestDTO.name().trim().toLowerCase());
        category.setDescription(updateCategoryRequestDTO.description() != null ? updateCategoryRequestDTO.description().trim() : null);
    }
}
