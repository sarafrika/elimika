package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseBundleCourseDTO;
import apps.sarafrika.elimika.course.model.CourseBundleCourse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseBundleCourseFactory {

    /**
     * Convert CourseBundleCourse entity to CourseBundleCourseDTO.
     *
     * @param courseBundleCourse the course bundle course entity
     * @return course bundle course DTO
     */
    public static CourseBundleCourseDTO toDTO(CourseBundleCourse courseBundleCourse) {
        if (courseBundleCourse == null) {
            return null;
        }
        
        return new CourseBundleCourseDTO(
                courseBundleCourse.getUuid(),
                courseBundleCourse.getBundleUuid(),
                courseBundleCourse.getCourseUuid(),
                courseBundleCourse.getSequenceOrder(),
                courseBundleCourse.getIsRequired(),
                courseBundleCourse.getCreatedDate(),
                courseBundleCourse.getCreatedBy(),
                courseBundleCourse.getLastModifiedDate(),
                courseBundleCourse.getLastModifiedBy()
        );
    }

    /**
     * Convert CourseBundleCourseDTO to CourseBundleCourse entity.
     *
     * @param dto the course bundle course DTO
     * @return course bundle course entity
     */
    public static CourseBundleCourse toEntity(CourseBundleCourseDTO dto) {
        if (dto == null) {
            return null;
        }

        CourseBundleCourse courseBundleCourse = new CourseBundleCourse();
        courseBundleCourse.setUuid(dto.uuid());
        courseBundleCourse.setBundleUuid(dto.bundleUuid());
        courseBundleCourse.setCourseUuid(dto.courseUuid());
        courseBundleCourse.setSequenceOrder(dto.sequenceOrder());
        courseBundleCourse.setIsRequired(dto.isRequired());
        courseBundleCourse.setCreatedDate(dto.createdDate());
        courseBundleCourse.setCreatedBy(dto.createdBy());
        courseBundleCourse.setLastModifiedDate(dto.updatedDate());
        courseBundleCourse.setLastModifiedBy(dto.updatedBy());

        return courseBundleCourse;
    }

    /**
     * Update existing CourseBundleCourse entity with data from DTO.
     *
     * @param existingAssociation the existing course bundle course entity
     * @param dto the course bundle course DTO with updated data
     */
    public static void updateEntityFromDTO(CourseBundleCourse existingAssociation, CourseBundleCourseDTO dto) {
        if (existingAssociation == null || dto == null) {
            return;
        }

        if (dto.bundleUuid() != null) {
            existingAssociation.setBundleUuid(dto.bundleUuid());
        }
        if (dto.courseUuid() != null) {
            existingAssociation.setCourseUuid(dto.courseUuid());
        }
        if (dto.sequenceOrder() != null) {
            existingAssociation.setSequenceOrder(dto.sequenceOrder());
        }
        if (dto.isRequired() != null) {
            existingAssociation.setIsRequired(dto.isRequired());
        }
    }
}