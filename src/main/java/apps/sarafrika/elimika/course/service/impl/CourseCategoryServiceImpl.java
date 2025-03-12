package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.CategoryNotFoundException;
import apps.sarafrika.elimika.course.config.exception.CourseCategoryNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateCategoryRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseCategoryRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CategoryResponseDTO;
import apps.sarafrika.elimika.course.persistence.CourseCategory;
import apps.sarafrika.elimika.course.persistence.CourseCategoryFactory;
import apps.sarafrika.elimika.course.persistence.CourseCategoryRepository;
import apps.sarafrika.elimika.course.service.CategoryService;
import apps.sarafrika.elimika.course.service.CourseCategoryService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static apps.sarafrika.elimika.course.service.impl.CategoryServiceImpl.ERROR_CATEGORY_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CourseCategoryServiceImpl implements CourseCategoryService {

    private static final String ERROR_COURSE_CATEGORY_NOT_FOUND = "Course category not found.";
    private static final String COURSE_CATEGORY_FOUND_SUCCESS = "Course category retrieved successfully.";

    private final CategoryService categoryService;
    private final CourseCategoryRepository courseCategoryRepository;

    @Transactional
    @Override
    public ResponseDTO<List<CategoryResponseDTO>> createCourseCategories(Long courseId, List<CategoryResponseDTO> categories) {

        List<CourseCategory> courseCategories = categories.stream()
                .map(categoryResponseDTO -> CourseCategoryFactory.createCourseCategory(courseId, categoryResponseDTO.id()))
                .toList();

        List<CourseCategory> savedCourseCategories = courseCategoryRepository.saveAll(courseCategories);

        List<Long> categoryIds = savedCourseCategories.stream()
                .map(CourseCategory::getCategoryId)
                .toList();

        return categoryService.findCategoriesByIds(categoryIds);
    }

    @Transactional
    @Override
    public ResponseDTO<List<CategoryResponseDTO>> updateCourseCategories(Long courseId, List<UpdateCourseCategoryRequestDTO> categories) {

        List<UpdateCourseCategoryRequestDTO> existingCategories = categories.stream()
                .filter(categoryResponseDTO -> categoryResponseDTO.id() != null)
                .toList();

        List<UpdateCourseCategoryRequestDTO> newCategories = categories.stream()
                .filter(categoryResponseDTO -> categoryResponseDTO.id() == null)
                .toList();

        List<CategoryResponseDTO> createdCategories = new ArrayList<>();

        if (!newCategories.isEmpty()) {

            List<CreateCategoryRequestDTO> newCategoryRequestDTOS = newCategories.stream()
                    .map(newCategory -> new CreateCategoryRequestDTO(newCategory.name(), newCategory.description()))
                    .toList();

            createdCategories = categoryService.createCategories(newCategoryRequestDTOS).data();
        }

        List<CategoryResponseDTO> categoriesToUpdateTo = new ArrayList<>(existingCategories.stream().map(category -> new CategoryResponseDTO(category.id(), category.name(), category.description())).toList());
        categoriesToUpdateTo.addAll(createdCategories);

        Set<Long> categoriesToUpdateToIds = categoriesToUpdateTo.stream().map(CategoryResponseDTO::id).collect(Collectors.toSet());

        ResponseDTO<List<CategoryResponseDTO>> foundCategories = categoryService.findCategoriesByIds(new ArrayList<>(categoriesToUpdateToIds));
        if (foundCategories.data().size() != categoriesToUpdateToIds.size()) {

            throw new CategoryNotFoundException(ERROR_CATEGORY_NOT_FOUND);
        }

        List<CourseCategory> currentCategories = courseCategoryRepository.findByCourseId(courseId);
        Set<Long> currentCategoryIds = currentCategories.stream().map(CourseCategory::getCategoryId).collect(Collectors.toSet());

        List<CourseCategory> categoriesToDelete = currentCategories.stream()
                .filter(courseCategory -> !categoriesToUpdateToIds.contains(courseCategory.getCategoryId()))
                .toList();

        List<CourseCategory> categoriesToAdd = categoriesToUpdateTo.stream()
                .filter(categoryResponseDTO -> !currentCategoryIds.contains(categoryResponseDTO.id()))
                .map(categoryResponseDTO -> CourseCategoryFactory.createCourseCategory(courseId, categoryResponseDTO.id()))
                .toList();

        if (!categoriesToDelete.isEmpty()) {
            courseCategoryRepository.deleteAll(categoriesToDelete);
        }

        if (!categoriesToAdd.isEmpty()) {
            courseCategoryRepository.saveAll(categoriesToAdd);
        }

        return categoryService.findCategoriesByIds(categoriesToUpdateToIds.stream().toList());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<List<CategoryResponseDTO>> findCourseCategoriesByCourseId(Long courseId) {

        List<CourseCategory> courseCategories = courseCategoryRepository.findByCourseId(courseId);

        List<Long> categoryIds = courseCategories.stream()
                .map(CourseCategory::getCategoryId)
                .toList();

        if (categoryIds.isEmpty()) {
            return new ResponseDTO<>(Collections.emptyList(), HttpStatus.OK.value(), COURSE_CATEGORY_FOUND_SUCCESS, null, LocalDateTime.now());
        }

        return categoryService.findCategoriesByIds(categoryIds);
    }

    private CourseCategory findCourseCategoryById(Long id) {

        return courseCategoryRepository.findById(id)
                .orElseThrow(() -> new CourseCategoryNotFoundException(ERROR_COURSE_CATEGORY_NOT_FOUND));
    }

    @Transactional
    @Override
    public void deleteCourseCategory(Long courseId) {

        CourseCategory courseCategory = findCourseCategoryById(courseId);

        courseCategoryRepository.delete(courseCategory);
    }
}
