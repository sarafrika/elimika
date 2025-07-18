package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.course.dto.CourseCategoryMappingDTO;
import apps.sarafrika.elimika.course.factory.CourseCategoryMappingFactory;
import apps.sarafrika.elimika.course.model.CourseCategoryMapping;
import apps.sarafrika.elimika.course.repository.CategoryRepository;
import apps.sarafrika.elimika.course.repository.CourseCategoryMappingRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseCategoryServiceImpl implements CourseCategoryService {

    private final CourseCategoryMappingRepository mappingRepository;
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public CourseCategoryMappingDTO addCategoryToCourse(UUID courseUuid, UUID categoryUuid) {
        log.debug("Adding category {} to course {}", categoryUuid, courseUuid);

        // Validate course exists
        if (!courseRepository.existsByUuid(courseUuid)) {
            throw new ResourceNotFoundException(String.format("Course with UUID %s not found", courseUuid));
        }

        // Validate category exists
        if (!categoryRepository.existsByUuid(categoryUuid)) {
            throw new ResourceNotFoundException(String.format("Category with UUID %s not found", categoryUuid));
        }

        // Check if mapping already exists
        if (mappingRepository.existsByCourseUuidAndCategoryUuid(courseUuid, categoryUuid)) {
            log.warn("Course {} is already assigned to category {}", courseUuid, categoryUuid);
            return mappingRepository.findByCourseUuidAndCategoryUuid(courseUuid, categoryUuid)
                    .map(CourseCategoryMappingFactory::toDTO)
                    .orElse(null);
        }

        // Create new mapping
        CourseCategoryMapping mapping = CourseCategoryMapping.builder()
                .courseUuid(courseUuid)
                .categoryUuid(categoryUuid)
                .build();

        CourseCategoryMapping savedMapping = mappingRepository.save(mapping);
        log.info("Successfully added category {} to course {}", categoryUuid, courseUuid);

        return CourseCategoryMappingFactory.toDTO(savedMapping);
    }

    @Override
    public List<CourseCategoryMappingDTO> addCategoriesToCourse(UUID courseUuid, Set<UUID> categoryUuids) {
        log.debug("Adding {} categories to course {}", categoryUuids.size(), courseUuid);

        // Validate course exists
        if (!courseRepository.existsByUuid(courseUuid)) {
            throw new ResourceNotFoundException(String.format("Course with UUID %s not found", courseUuid));
        }

        List<CourseCategoryMappingDTO> results = new ArrayList<>();

        for (UUID categoryUuid : categoryUuids) {
            try {
                CourseCategoryMappingDTO mapping = addCategoryToCourse(courseUuid, categoryUuid);
                if (mapping != null) {
                    results.add(mapping);
                }
            } catch (Exception e) {
                log.warn("Failed to add category {} to course {}: {}", categoryUuid, courseUuid, e.getMessage());
                // Continue with other categories instead of failing entirely
            }
        }

        log.info("Successfully added {} categories to course {}", results.size(), courseUuid);
        return results;
    }

    @Override
    public void removeCategoryFromCourse(UUID courseUuid, UUID categoryUuid) {
        log.debug("Removing category {} from course {}", categoryUuid, courseUuid);

        if (!mappingRepository.existsByCourseUuidAndCategoryUuid(courseUuid, categoryUuid)) {
            log.warn("No mapping found between course {} and category {}", courseUuid, categoryUuid);
            return;
        }

        mappingRepository.deleteByCourseUuidAndCategoryUuid(courseUuid, categoryUuid);
        log.info("Successfully removed category {} from course {}", categoryUuid, courseUuid);
    }

    @Override
    public void removeCategoriesFromCourse(UUID courseUuid, Set<UUID> categoryUuids) {
        log.debug("Removing {} categories from course {}", categoryUuids.size(), courseUuid);

        for (UUID categoryUuid : categoryUuids) {
            removeCategoryFromCourse(courseUuid, categoryUuid);
        }

        log.info("Successfully removed {} categories from course {}", categoryUuids.size(), courseUuid);
    }

    @Override
    public List<CourseCategoryMappingDTO> updateCourseCategories(UUID courseUuid, Set<UUID> categoryUuids) {
        log.debug("Updating categories for course {} with {} new categories", courseUuid,
                categoryUuids != null ? categoryUuids.size() : 0);

        // Remove all existing categories first
        removeAllCategoriesFromCourse(courseUuid);

        // Add new categories if provided
        if (categoryUuids != null && !categoryUuids.isEmpty()) {
            return addCategoriesToCourse(courseUuid, categoryUuids);
        }

        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCategoryMappingDTO> getCourseCategoryMappings(UUID courseUuid) {
        return mappingRepository.findByCourseUuid(courseUuid)
                .stream()
                .map(CourseCategoryMappingFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategoryNamesByCourse(UUID courseUuid) {
        return mappingRepository.findCategoryNamesByCourseUuid(courseUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCategoryMappingDTO> getCategoryCourseMappings(UUID categoryUuid) {
        return mappingRepository.findByCategoryUuid(categoryUuid)
                .stream()
                .map(CourseCategoryMappingFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getCourseUuidsByCategory(UUID categoryUuid) {
        return mappingRepository.findCourseUuidsByCategoryUuid(categoryUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourseInCategory(UUID courseUuid, UUID categoryUuid) {
        return mappingRepository.existsByCourseUuidAndCategoryUuid(courseUuid, categoryUuid);
    }

    @Override
    public void removeAllCategoriesFromCourse(UUID courseUuid) {
        log.debug("Removing all categories from course {}", courseUuid);

        long deletedCount = mappingRepository.countByCourseUuid(courseUuid);
        mappingRepository.deleteByCourseUuid(courseUuid);

        log.info("Successfully removed {} categories from course {}", deletedCount, courseUuid);
    }

    @Override
    public void removeCourseFromAllCategories(UUID courseUuid) {
        // This is the same as removeAllCategoriesFromCourse
        removeAllCategoriesFromCourse(courseUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCategoryCountForCourse(UUID courseUuid) {
        return mappingRepository.countByCourseUuid(courseUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCourseCountForCategory(UUID categoryUuid) {
        return mappingRepository.countByCategoryUuid(categoryUuid);
    }
}