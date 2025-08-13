package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseRubricAssociationDTO;
import apps.sarafrika.elimika.course.model.CourseRubricAssociation;

/**
 * Factory class for converting between CourseRubricAssociation entities and DTOs
 * <p>
 * Provides utility methods for mapping between the entity and DTO representations
 * of course-rubric associations.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
public class CourseRubricAssociationFactory {

    /**
     * Converts a CourseRubricAssociation entity to its DTO representation.
     *
     * @param association the entity to convert
     * @return the DTO representation
     */
    public static CourseRubricAssociationDTO toDTO(CourseRubricAssociation association) {
        if (association == null) {
            return null;
        }

        return new CourseRubricAssociationDTO(
                association.getUuid(),
                association.getCourseUuid(),
                association.getRubricUuid(),
                association.getAssociatedBy(),
                association.getAssociationDate(),
                association.getIsPrimaryRubric(),
                association.getUsageContext(),
                association.getCreatedDate(),
                association.getCreatedBy(),
                association.getLastModifiedDate(),
                association.getLastModifiedBy()
        );
    }

    /**
     * Converts a CourseRubricAssociationDTO to its entity representation.
     *
     * @param dto the DTO to convert
     * @return the entity representation
     */
    public static CourseRubricAssociation toEntity(CourseRubricAssociationDTO dto) {
        if (dto == null) {
            return null;
        }

        CourseRubricAssociation association = new CourseRubricAssociation();
        updateEntityFromDTO(association, dto);
        return association;
    }

    /**
     * Updates an existing CourseRubricAssociation entity with values from a DTO.
     *
     * @param association the entity to update
     * @param dto the DTO containing the new values
     */
    public static void updateEntityFromDTO(CourseRubricAssociation association, CourseRubricAssociationDTO dto) {
        if (association == null || dto == null) {
            return;
        }

        association.setCourseUuid(dto.courseUuid());
        association.setRubricUuid(dto.rubricUuid());
        association.setAssociatedBy(dto.associatedBy());
        association.setAssociationDate(dto.associationDate());
        association.setIsPrimaryRubric(dto.isPrimaryRubric());
        association.setUsageContext(dto.usageContext());
        association.setCreatedDate(dto.createdDate());
        association.setCreatedBy(dto.createdBy());
        association.setLastModifiedDate(dto.updatedDate());
        association.setLastModifiedBy(dto.updatedBy());
    }
}