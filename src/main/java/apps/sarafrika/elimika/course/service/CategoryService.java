package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CategoryRequestDTO;
import apps.sarafrika.elimika.course.dto.request.CreateCategoryRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCategoryRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CategoryResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {

    ResponsePageableDTO<CategoryResponseDTO> findAllCategories(CategoryRequestDTO categoryRequestDTO, Pageable pageable);

    ResponseDTO<CategoryResponseDTO> findCategory(Long id);

    ResponseDTO<List<CategoryResponseDTO>> findCategoriesByIds(List<Long> ids);

    ResponseDTO<CategoryResponseDTO> createCategory(CreateCategoryRequestDTO createCategoryRequestDTO);

    ResponseDTO<List<CategoryResponseDTO>> createCategories(List<CreateCategoryRequestDTO> createCategoryRequestDTOS);

    ResponseDTO<CategoryResponseDTO> updateCategory(UpdateCategoryRequestDTO updateCategoryRequestDTO, Long id);

    void deleteCategory(Long id);
}
