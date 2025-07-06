package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.ProgramCourseDTO;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProgramCourseFactory {

    // Convert ProgramCourse entity to ProgramCourseDTO
    public static ProgramCourseDTO toDTO(ProgramCourse programCourse) {
        if (programCourse == null) {
            return null;
        }
        return new ProgramCourseDTO(
                programCourse.getUuid(),
                programCourse.getProgramUuid(),
                programCourse.getCourseUuid(),
                programCourse.getSequenceOrder(),
                programCourse.getIsRequired(),
                programCourse.getPrerequisiteCourseUuid(),
                programCourse.getCreatedDate(),
                programCourse.getCreatedBy(),
                programCourse.getLastModifiedDate(),
                programCourse.getLastModifiedBy()
        );
    }

    // Convert ProgramCourseDTO to ProgramCourse entity
    public static ProgramCourse toEntity(ProgramCourseDTO dto) {
        if (dto == null) {
            return null;
        }
        ProgramCourse programCourse = new ProgramCourse();
        programCourse.setUuid(dto.uuid());
        programCourse.setProgramUuid(dto.programUuid());
        programCourse.setCourseUuid(dto.courseUuid());
        programCourse.setSequenceOrder(dto.sequenceOrder());
        programCourse.setIsRequired(dto.isRequired());
        programCourse.setPrerequisiteCourseUuid(dto.prerequisiteCourseUuid());
        programCourse.setCreatedDate(dto.createdDate());
        programCourse.setCreatedBy(dto.createdBy());
        programCourse.setLastModifiedDate(dto.updatedDate());
        programCourse.setLastModifiedBy(dto.updatedBy());
        return programCourse;
    }
}