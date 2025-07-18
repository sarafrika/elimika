package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseCategoryMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseFactory {

    /**
     * Convert Course entity to CourseDTO with category information
     */
    public static CourseDTO toDTO(Course course) {
        if (course == null) {
            return null;
        }

        Set<UUID> categoryUuids = course.getCategoryMappings() != null ?
                course.getCategoryMappings().stream()
                        .map(CourseCategoryMapping::getCategoryUuid)
                        .collect(Collectors.toSet()) : new HashSet<>();

        List<String> categoryNames = course.getCategoryMappings() != null ?
                course.getCategoryMappings().stream()
                        .map(CourseCategoryMapping::getCategory)
                        .filter(category -> category != null)
                        .map(apps.sarafrika.elimika.course.model.Category::getName)
                        .sorted()
                        .collect(Collectors.toList()) : List.of();

        return new CourseDTO(
                course.getUuid(),
                course.getName(),
                course.getInstructorUuid(),
                categoryUuids.isEmpty() ? null : categoryUuids,
                course.getDifficultyUuid(),
                course.getDescription(),
                course.getObjectives(),
                course.getPrerequisites(),
                course.getDurationHours(),
                course.getDurationMinutes(),
                course.getClassLimit(),
                course.getPrice(),
                course.getAgeLowerLimit(),
                course.getAgeUpperLimit(),
                course.getThumbnailUrl(),
                course.getIntroVideoUrl(),
                course.getBannerUrl(),
                course.getStatus(),
                course.getActive(),
                categoryNames.isEmpty() ? null : categoryNames,
                course.getCreatedDate(),
                course.getCreatedBy(),
                course.getLastModifiedDate(),
                course.getLastModifiedBy()
        );
    }

    /**
     * Convert Course entity to CourseDTO with provided category names
     * This method is useful when category names are fetched separately
     */
    public static CourseDTO toDTO(Course course, List<String> categoryNames) {
        if (course == null) {
            return null;
        }

        // Extract category UUIDs from mappings
        Set<UUID> categoryUuids = course.getCategoryMappings() != null ?
                course.getCategoryMappings().stream()
                        .map(CourseCategoryMapping::getCategoryUuid)
                        .collect(Collectors.toSet()) : new HashSet<>();

        return new CourseDTO(
                course.getUuid(),
                course.getName(),
                course.getInstructorUuid(),
                categoryUuids.isEmpty() ? null : categoryUuids,
                course.getDifficultyUuid(),
                course.getDescription(),
                course.getObjectives(),
                course.getPrerequisites(),
                course.getDurationHours(),
                course.getDurationMinutes(),
                course.getClassLimit(),
                course.getPrice(),
                course.getAgeLowerLimit(),
                course.getAgeUpperLimit(),
                course.getThumbnailUrl(),
                course.getIntroVideoUrl(),
                course.getBannerUrl(),
                course.getStatus(),
                course.getActive(),
                categoryNames,
                course.getCreatedDate(),
                course.getCreatedBy(),
                course.getLastModifiedDate(),
                course.getLastModifiedBy()
        );
    }

    /**
     * Convert CourseDTO to Course entity
     * Note: This doesn't handle category mappings - those need to be managed separately
     */
    public static Course toEntity(CourseDTO dto) {
        if (dto == null) {
            return null;
        }
        Course course = new Course();
        course.setUuid(dto.uuid());
        course.setName(dto.name());
        course.setInstructorUuid(dto.instructorUuid());
        course.setDifficultyUuid(dto.difficultyUuid());
        course.setDescription(dto.description());
        course.setObjectives(dto.objectives());
        course.setPrerequisites(dto.prerequisites());
        course.setDurationHours(dto.durationHours());
        course.setDurationMinutes(dto.durationMinutes());
        course.setClassLimit(dto.classLimit());
        course.setPrice(dto.price());
        course.setThumbnailUrl(dto.thumbnailUrl());
        course.setIntroVideoUrl(dto.introVideoUrl());
        course.setBannerUrl(dto.bannerUrl());
        course.setStatus(dto.status());
        course.setActive(dto.active());
        course.setCreatedDate(dto.createdDate());
        course.setCreatedBy(dto.createdBy());
        course.setLastModifiedDate(dto.updatedDate());
        course.setLastModifiedBy(dto.updatedBy());
        course.setAgeUpperLimit(dto.ageUpperLimit());
        course.setAgeLowerLimit(dto.ageLowerLimit());

        // Initialize empty category mappings set
        course.setCategoryMappings(new HashSet<>());

        return course;
    }

    /**
     * Create a minimal CourseDTO for listing purposes
     */
    public static CourseDTO toListDTO(Course course, List<String> categoryNames) {
        if (course == null) {
            return null;
        }

        Set<UUID> categoryUuids = course.getCategoryMappings() != null ?
                course.getCategoryMappings().stream()
                        .map(CourseCategoryMapping::getCategoryUuid)
                        .collect(Collectors.toSet()) : new HashSet<>();

        return new CourseDTO(
                course.getUuid(),
                course.getName(),
                course.getInstructorUuid(),
                categoryUuids.isEmpty() ? null : categoryUuids,
                course.getDifficultyUuid(),
                course.getDescription(),
                null, // objectives not needed for listing
                null, // prerequisites not needed for listing
                course.getDurationHours(),
                course.getDurationMinutes(),
                course.getClassLimit(),
                course.getPrice(),
                course.getAgeLowerLimit(),
                course.getAgeUpperLimit(),
                course.getThumbnailUrl(),
                null, // intro video not needed for listing
                course.getBannerUrl(),
                course.getStatus(),
                course.getActive(),
                categoryNames,
                course.getCreatedDate(),
                course.getCreatedBy(),
                course.getLastModifiedDate(),
                course.getLastModifiedBy()
        );
    }
}