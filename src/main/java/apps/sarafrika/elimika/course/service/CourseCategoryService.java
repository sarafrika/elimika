package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.UpdateCourseCategoryRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CategoryResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;

import java.util.List;

public interface CourseCategoryService {

    ResponseDTO<List<CategoryResponseDTO>> createCourseCategories(Long courseId, List<CategoryResponseDTO> categories);

    ResponseDTO<List<CategoryResponseDTO>> updateCourseCategories(Long courseId, List<UpdateCourseCategoryRequestDTO> categories);

    ResponseDTO<List<CategoryResponseDTO>> findCourseCategoriesByCourseId(Long courseId);

    void deleteCourseCategory(Long courseId);
}
