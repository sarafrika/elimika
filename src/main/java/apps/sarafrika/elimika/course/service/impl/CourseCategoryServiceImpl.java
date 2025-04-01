package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseCategoryDTO;
import apps.sarafrika.elimika.course.mappers.CourseCategoryMapper;
import apps.sarafrika.elimika.course.model.Category;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseCategory;
import apps.sarafrika.elimika.course.repository.CategoryRepository;
import apps.sarafrika.elimika.course.repository.CourseCategoryRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the CourseCategoryService interface.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseCategoryServiceImpl implements CourseCategoryService {

    private static final String ERROR_COURSE_CATEGORY_NOT_FOUND = "Course category not found.";
    private static final String ERROR_COURSE_NOT_FOUND = "Course not found.";
    private static final String ERROR_CATEGORY_NOT_FOUND = "Category not found.";
    private static final String ERROR_COURSE_CATEGORY_EXISTS = "This course is already associated with the specified category.";

    private final CourseCategoryRepository courseCategoryRepository;
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final GenericSpecificationBuilder<CourseCategory> specificationBuilder;

    @Override
    @Transactional
    public CourseCategoryDTO createCourseCategory(CourseCategoryDTO courseCategoryDTO) {
        log.info("Creating course category association for courseUuid: {} and categoryUuid: {}",
                courseCategoryDTO.courseUuid(), courseCategoryDTO.categoryUuid());

        // Validate that both course and category exist
        Course course = getCourseOrThrow(courseCategoryDTO.courseUuid());
        Category category = getCategoryOrThrow(courseCategoryDTO.categoryUuid());

        // Check if association already exists
        if (courseCategoryRepository.existsByCourseUuidAndCategoryUuid(
                courseCategoryDTO.courseUuid(), courseCategoryDTO.categoryUuid())) {
            log.error("Course category association already exists for courseUuid: {} and categoryUuid: {}",
                    courseCategoryDTO.courseUuid(), courseCategoryDTO.categoryUuid());
            throw new DataIntegrityViolationException(ERROR_COURSE_CATEGORY_EXISTS);
        }

        // Create and save the association
        CourseCategory courseCategory = new CourseCategory();
        courseCategory.setCourseUuid(course.getUuid());
        courseCategory.setCategoryUuid(category.getUuid());

        CourseCategory savedCourseCategory = courseCategoryRepository.save(courseCategory);
        log.info("Course category association created with UUID: {}", savedCourseCategory.getUuid());

        return CourseCategoryMapper.toDto(savedCourseCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCategoryDTO getCourseCategoryByUuid(UUID uuid) {
        log.info("Fetching course category with UUID: {}", uuid);

        CourseCategory courseCategory = getCourseCategoryOrThrow(uuid);
        return CourseCategoryMapper.toDto(courseCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCategoryDTO> getAllCourseCategories(Pageable pageable) {
        log.info("Fetching all course categories with pagination");

        Page<CourseCategory> courseCategories = courseCategoryRepository.findAll(pageable);
        return courseCategories.map(CourseCategoryMapper::toDto);
    }

    @Override
    @Transactional
    public CourseCategoryDTO updateCourseCategory(UUID uuid, CourseCategoryDTO courseCategoryDTO) {
        log.info("Updating course category with UUID: {}", uuid);

        CourseCategory courseCategory = getCourseCategoryOrThrow(uuid);

        // Validate that both course and category exist if they're being changed
        if (courseCategoryDTO.courseUuid() != null &&
                !courseCategory.getCourseUuid().equals(courseCategoryDTO.courseUuid())) {
            courseCategory.setCourseUuid(getCourseOrThrow(courseCategoryDTO.courseUuid()).getUuid());
        }

        if (courseCategoryDTO.categoryUuid() != null &&
                !courseCategory.getCategoryUuid().equals(courseCategoryDTO.categoryUuid())) {
            // Check for duplicate if changing category
            if (courseCategoryRepository.existsByCourseUuidAndCategoryUuid(
                    courseCategory.getCourseUuid(), courseCategoryDTO.categoryUuid())) {
                log.error("Cannot update: Course is already associated with the specified category");
                throw new DataIntegrityViolationException(ERROR_COURSE_CATEGORY_EXISTS);
            }
            courseCategory.setCategoryUuid(getCategoryOrThrow(courseCategoryDTO.categoryUuid()).getUuid());
        }

        CourseCategory updatedCourseCategory = courseCategoryRepository.save(courseCategory);
        log.info("Course category updated: {}", updatedCourseCategory.getUuid());

        return CourseCategoryMapper.toDto(updatedCourseCategory);
    }

    @Override
    @Transactional
    public void deleteCourseCategory(UUID uuid) {
        log.info("Deleting course category with UUID: {}", uuid);

        CourseCategory courseCategory = getCourseCategoryOrThrow(uuid);
        courseCategoryRepository.delete(courseCategory);

        log.info("Course category deleted: {}", uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCategoryDTO> searchCourseCategories(Map<String, String> searchParams, Pageable pageable) {
        log.info("Searching course categories with parameters: {}", searchParams);

        Specification<CourseCategory> specification =
                specificationBuilder.buildSpecification(CourseCategory.class, searchParams);
        Page<CourseCategory> courseCategories = courseCategoryRepository.findAll(specification, pageable);

        return courseCategories.map(CourseCategoryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCategoryDTO> getCategoriesByCourseUuid(UUID courseUuid) {
        log.info("Fetching categories for course with UUID: {}", courseUuid);

        List<CourseCategory> courseCategories = courseCategoryRepository.findByCourseUuid(courseUuid);
        return courseCategories.stream()
                .map(CourseCategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveCourseCategories(UUID courseUuid, List<CourseCategoryDTO> categories) {
        log.info("Saving categories for course with UUID: {}", courseUuid);

        // Get the course
        Course course = getCourseOrThrow(courseUuid);

        // Use non-transactional helper to process the categories
        saveCourseCategoriesInternal(course, categories);

        log.info("Categories saved for course: {}", courseUuid);
    }

    @Override
    @Transactional
    public void updateCourseCategories(UUID courseUuid, List<CourseCategoryDTO> categories) {
        log.info("Updating categories for course with UUID: {}", courseUuid);

        // Delete existing categories (using repository method directly)
        courseCategoryRepository.deleteByCourseUuid(courseUuid);

        // Get the course (it's definitely needed for saving)
        Course course = getCourseOrThrow(courseUuid);

        // Use non-transactional helper to save new categories
        saveCourseCategoriesInternal(course, categories);

        log.info("Categories updated for course: {}", courseUuid);
    }

    @Override
    @Transactional
    public void deleteCategoriesByCourseUuid(UUID courseUuid) {
        log.info("Deleting all categories for course with UUID: {}", courseUuid);

        // Call repository method directly
        courseCategoryRepository.deleteByCourseUuid(courseUuid);

        log.info("All categories deleted for course: {}", courseUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCourseCategory(UUID courseUuid, UUID categoryUuid) {
        return courseCategoryRepository.existsByCourseUuidAndCategoryUuid(courseUuid, categoryUuid);
    }

    // Non-transactional helper methods

    /**
     * Helper method to save course categories without transaction boundaries.
     * This supports both saveCourseCategories and updateCourseCategories methods.
     *
     * @param course The course entity
     * @param categories The list of category DTOs to associate with the course
     */
    private void saveCourseCategoriesInternal(Course course, List<CourseCategoryDTO> categories) {
        // Process each category
        for (CourseCategoryDTO categoryDTO : categories) {
            // Only process valid categories that don't already have associations
            if (categoryDTO.categoryUuid() != null &&
                    !courseCategoryRepository.existsByCourseUuidAndCategoryUuid(
                            course.getUuid(), categoryDTO.categoryUuid())) {

                // Get the category
                Category category = getCategoryOrThrow(categoryDTO.categoryUuid());

                // Create and save the association
                CourseCategory courseCategory = new CourseCategory();
                courseCategory.setCourseUuid(course.getUuid());
                courseCategory.setCategoryUuid(category.getUuid());
                courseCategoryRepository.save(courseCategory);

                log.debug("Created category association for course: {} and category: {}",
                        course.getUuid(), categoryDTO.categoryUuid());
            } else if (categoryDTO.categoryUuid() == null) {
                log.warn("Skipping category with null UUID");
            } else {
                log.debug("Category association already exists for course: {} and category: {}",
                        course.getUuid(), categoryDTO.categoryUuid());
            }
        }
    }

    /**
     * Gets a course by UUID or throws an exception if not found.
     *
     * @param uuid The UUID of the course.
     * @return The found Course entity.
     * @throws RecordNotFoundException if the course is not found.
     */
    private Course getCourseOrThrow(UUID uuid) {
        return courseRepository.findByUuid(uuid)
                .orElseThrow(() -> {
                    log.error("Course with UUID '{}' not found", uuid);
                    return new RecordNotFoundException(ERROR_COURSE_NOT_FOUND);
                });
    }

    /**
     * Gets a category by UUID or throws an exception if not found.
     *
     * @param uuid The UUID of the category.
     * @return The found Category entity.
     * @throws RecordNotFoundException if the category is not found.
     */
    private Category getCategoryOrThrow(UUID uuid) {
        return categoryRepository.findByUuid(uuid)
                .orElseThrow(() -> new RecordNotFoundException(ERROR_CATEGORY_NOT_FOUND));
    }

    /**
     * Gets a course category by UUID or throws an exception if not found.
     *
     * @param uuid The UUID of the course category.
     * @return The found CourseCategory entity.
     * @throws RecordNotFoundException if the course category is not found.
     */
    private CourseCategory getCourseCategoryOrThrow(UUID uuid) {
        return courseCategoryRepository.findByUuid(uuid)
                .orElseThrow(() -> new RecordNotFoundException(ERROR_COURSE_CATEGORY_NOT_FOUND));
    }
}