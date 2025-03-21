package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.model.Course;

import java.util.ArrayList;

/**
 * Utility class for mapping between Course entities and DTOs
 */
public class CourseMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private CourseMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Converts a Course entity to a CourseDTO
     *
     * @param entity the Course entity to convert
     * @return the corresponding CourseDTO
     */
    public static CourseDTO toDto(Course entity) {
        if (entity == null) {
            return null;
        }

        return CourseDTO.builder()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .thumbnailUrl(entity.getThumbnailUrl())
                .durationHours(entity.getDurationHours())
                .difficultyLevel(entity.getDifficultyLevel())
                .isFree(entity.isFree())
                .originalPrice(entity.getOriginalPrice())
                .salePrice(entity.getSalePrice())
                .minAge(entity.getMinAge())
                .maxAge(entity.getMaxAge())
                .classSize(entity.getClassSize())
                .instructors(new ArrayList<>()) // needs to be populated separately
                .learningObjectives(new ArrayList<>()) // needs to be populated separately
                .courseCategories(new ArrayList<>()) // needs to be populated separately
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Converts a CourseDTO to a Course entity
     *
     * @param dto the CourseDTO to convert
     * @return the corresponding Course entity
     */
    public static Course toEntity(CourseDTO dto) {
        if (dto == null) {
            return null;
        }

        Course course = new Course();
        course.setUuid(dto.uuid());
        course.setName(dto.name());
        course.setCode(dto.code());
        course.setDescription(dto.description());
        course.setThumbnailUrl(dto.thumbnailUrl());
        course.setDurationHours(dto.durationHours());
        course.setDifficultyLevel(dto.difficultyLevel());
        course.setFree(dto.isFree());
        course.setOriginalPrice(dto.originalPrice());
        course.setSalePrice(dto.salePrice());
        course.setMinAge(dto.minAge());
        course.setMaxAge(dto.maxAge());
        course.setClassSize(dto.classSize());

        return course;
    }
}