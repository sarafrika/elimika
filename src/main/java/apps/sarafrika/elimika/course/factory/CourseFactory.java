package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.dto.CourseTrainingRequirementDTO;
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

        List<String> categoryNames = extractCategoryNames(course);
        return toDTO(course, categoryNames.isEmpty() ? null : categoryNames, null);
    }

    /**
     * Convert Course entity to CourseDTO with provided category names
     * This method is useful when category names are fetched separately
     */
    public static CourseDTO toDTO(Course course, List<String> categoryNames) {
        return toDTO(course, categoryNames, null);
    }

    public static CourseDTO toDTO(Course course,
                                  List<String> categoryNames,
                                  List<CourseTrainingRequirementDTO> trainingRequirements) {
        if (course == null) {
            return null;
        }

        Set<UUID> categoryUuids = extractCategoryUuids(course);

        return new CourseDTO(
                course.getUuid(),
                course.getName(),
                course.getCourseCreatorUuid(),
                categoryUuids.isEmpty() ? null : categoryUuids,
                course.getDifficultyUuid(),
                course.getDescription(),
                course.getObjectives(),
                course.getPrerequisites(),
                course.getDurationHours(),
                course.getDurationMinutes(),
                course.getClassLimit(),
                course.getPrice(),
                course.getMinimumTrainingFee(),
                course.getCreatorSharePercentage(),
                course.getInstructorSharePercentage(),
                course.getRevenueShareNotes(),
                course.getAgeLowerLimit(),
                course.getAgeUpperLimit(),
                course.getThumbnailUrl(),
                course.getIntroVideoUrl(),
                course.getBannerUrl(),
                course.getStatus(),
                course.getActive(),
                course.getAdminApproved(),
                trainingRequirements,
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
        course.setCourseCreatorUuid(dto.courseCreatorUuid());
        course.setDifficultyUuid(dto.difficultyUuid());
        course.setDescription(dto.description());
        course.setObjectives(dto.objectives());
        course.setPrerequisites(dto.prerequisites());
        course.setDurationHours(dto.durationHours());
        course.setDurationMinutes(dto.durationMinutes());
        course.setClassLimit(dto.classLimit());
        course.setPrice(dto.price());
        course.setMinimumTrainingFee(dto.minimumTrainingFee());
        course.setCreatorSharePercentage(dto.creatorSharePercentage());
        course.setInstructorSharePercentage(dto.instructorSharePercentage());
        course.setRevenueShareNotes(dto.revenueShareNotes());
        course.setThumbnailUrl(dto.thumbnailUrl());
        course.setIntroVideoUrl(dto.introVideoUrl());
        course.setBannerUrl(dto.bannerUrl());
        course.setStatus(dto.status());
        course.setActive(dto.active());
        course.setAdminApproved(dto.adminApproved());
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

        return toDTO(course, categoryNames, null);
    }

    private static Set<UUID> extractCategoryUuids(Course course) {
        return course.getCategoryMappings() != null ?
                course.getCategoryMappings().stream()
                        .map(CourseCategoryMapping::getCategoryUuid)
                        .collect(Collectors.toSet()) : new HashSet<>();
    }

    private static List<String> extractCategoryNames(Course course) {
        return course.getCategoryMappings() != null ?
                course.getCategoryMappings().stream()
                        .map(CourseCategoryMapping::getCategory)
                        .filter(category -> category != null)
                        .map(apps.sarafrika.elimika.course.model.Category::getName)
                        .sorted()
                        .collect(Collectors.toList()) : List.of();
    }
}
