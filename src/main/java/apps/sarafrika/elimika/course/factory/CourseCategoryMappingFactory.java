package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseCategoryMappingDTO;
import apps.sarafrika.elimika.course.model.CourseCategoryMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseCategoryMappingFactory {

    /**
     * Convert CourseCategoryMapping entity to DTO
     */
    public static CourseCategoryMappingDTO toDTO(CourseCategoryMapping mapping) {
        if (mapping == null) {
            return null;
        }

        String courseName = null;
        String categoryName = null;

        // Extract names from loaded entities if available
        if (mapping.getCourse() != null) {
            courseName = mapping.getCourse().getName();
        }
        if (mapping.getCategory() != null) {
            categoryName = mapping.getCategory().getName();
        }

        return new CourseCategoryMappingDTO(
                mapping.getUuid(),
                mapping.getCourseUuid(),
                mapping.getCategoryUuid(),
                courseName,
                categoryName,
                mapping.getCreatedDate(),
                mapping.getCreatedBy(),
                mapping.getLastModifiedDate(),
                mapping.getLastModifiedBy()
        );
    }

    /**
     * Convert CourseCategoryMapping entity to DTO with provided names
     */
    public static CourseCategoryMappingDTO toDTO(CourseCategoryMapping mapping, String courseName, String categoryName) {
        if (mapping == null) {
            return null;
        }

        return new CourseCategoryMappingDTO(
                mapping.getUuid(),
                mapping.getCourseUuid(),
                mapping.getCategoryUuid(),
                courseName,
                categoryName,
                mapping.getCreatedDate(),
                mapping.getCreatedBy(),
                mapping.getLastModifiedDate(),
                mapping.getLastModifiedBy()
        );
    }

    /**
     * Convert DTO to CourseCategoryMapping entity
     */
    public static CourseCategoryMapping toEntity(CourseCategoryMappingDTO dto) {
        if (dto == null) {
            return null;
        }

        return CourseCategoryMapping.builder()
                .courseUuid(dto.courseUuid())
                .categoryUuid(dto.categoryUuid())
                .build();
    }

    /**
     * Create a minimal DTO for simple mappings
     */
    public static CourseCategoryMappingDTO createSimpleDTO(UUID courseUuid, UUID categoryUuid) {
        return new CourseCategoryMappingDTO(
                null, courseUuid, categoryUuid, null, null, null, null, null, null);
    }
}