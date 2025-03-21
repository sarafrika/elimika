package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseCategoryDTO;
import apps.sarafrika.elimika.course.repository.CourseCategoryRepository;
import apps.sarafrika.elimika.course.service.CategoryService;
import apps.sarafrika.elimika.course.service.CourseCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseCategoryServiceImpl implements CourseCategoryService {

    private static final String ERROR_COURSE_CATEGORY_NOT_FOUND = "Course category not found.";
    private static final String COURSE_CATEGORY_FOUND_SUCCESS = "Course category retrieved successfully.";

    private final CategoryService categoryService;
    private final CourseCategoryRepository courseCategoryRepository;

    @Override
    public CourseCategoryDTO createCourseCategory(CourseCategoryDTO courseCategoryDTO) {
        return null;
    }

    @Override
    public CourseCategoryDTO getCourseCategoryByUuid(UUID uuid) {
        return null;
    }

    @Override
    public Page<CourseCategoryDTO> getAllCourseCategories(Pageable pageable) {
        return null;
    }

    @Override
    public CourseCategoryDTO updateCourseCategory(UUID uuid, CourseCategoryDTO courseCategoryDTO) {
        return null;
    }

    @Override
    public void deleteCourseCategory(UUID uuid) {

    }

    @Override
    public Page<CourseCategoryDTO> searchCourseCategories(Map<String, String> searchParams, Pageable pageable) {
        return null;
    }
}
