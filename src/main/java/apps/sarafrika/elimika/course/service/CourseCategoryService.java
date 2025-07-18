package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseCategoryMappingDTO;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service interface for managing course-category relationships
 */
public interface CourseCategoryService {

    /**
     * Add a category to a course
     */
    CourseCategoryMappingDTO addCategoryToCourse(UUID courseUuid, UUID categoryUuid);

    /**
     * Add multiple categories to a course
     */
    List<CourseCategoryMappingDTO> addCategoriesToCourse(UUID courseUuid, Set<UUID> categoryUuids);

    /**
     * Remove a category from a course
     */
    void removeCategoryFromCourse(UUID courseUuid, UUID categoryUuid);

    /**
     * Remove multiple categories from a course
     */
    void removeCategoriesFromCourse(UUID courseUuid, Set<UUID> categoryUuids);

    /**
     * Update all categories for a course (replaces existing categories)
     */
    List<CourseCategoryMappingDTO> updateCourseCategories(UUID courseUuid, Set<UUID> categoryUuids);

    /**
     * Get all categories for a course
     */
    List<CourseCategoryMappingDTO> getCourseCategoryMappings(UUID courseUuid);

    /**
     * Get category names for a course
     */
    List<String> getCategoryNamesByCourse(UUID courseUuid);

    /**
     * Get all courses for a category
     */
    List<CourseCategoryMappingDTO> getCategoryCourseMappings(UUID categoryUuid);

    /**
     * Get course UUIDs for a category
     */
    List<UUID> getCourseUuidsByCategory(UUID categoryUuid);

    /**
     * Check if a course is in a specific category
     */
    boolean isCourseInCategory(UUID courseUuid, UUID categoryUuid);

    /**
     * Remove all categories from a course
     */
    void removeAllCategoriesFromCourse(UUID courseUuid);

    /**
     * Remove course from all categories
     */
    void removeCourseFromAllCategories(UUID courseUuid);

    /**
     * Get count of categories for a course
     */
    long getCategoryCountForCourse(UUID courseUuid);

    /**
     * Get count of courses for a category
     */
    long getCourseCountForCategory(UUID categoryUuid);
}