package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.ProgramEnrollmentDTO;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProgramEnrollmentFactory {

    // Convert ProgramEnrollment entity to ProgramEnrollmentDTO
    public static ProgramEnrollmentDTO toDTO(ProgramEnrollment programEnrollment) {
        if (programEnrollment == null) {
            return null;
        }
        return new ProgramEnrollmentDTO(
                programEnrollment.getUuid(),
                programEnrollment.getStudentUuid(),
                programEnrollment.getProgramUuid(),
                programEnrollment.getEnrollmentDate(),
                programEnrollment.getCompletionDate(),
                programEnrollment.getStatus(),
                programEnrollment.getProgressPercentage(),
                programEnrollment.getFinalGrade(),
                programEnrollment.getCreatedDate(),
                programEnrollment.getCreatedBy(),
                programEnrollment.getLastModifiedDate(),
                programEnrollment.getLastModifiedBy()
        );
    }

    // Convert ProgramEnrollmentDTO to ProgramEnrollment entity
    public static ProgramEnrollment toEntity(ProgramEnrollmentDTO dto) {
        if (dto == null) {
            return null;
        }
        ProgramEnrollment programEnrollment = new ProgramEnrollment();
        programEnrollment.setUuid(dto.uuid());
        programEnrollment.setStudentUuid(dto.studentUuid());
        programEnrollment.setProgramUuid(dto.programUuid());
        programEnrollment.setEnrollmentDate(dto.enrollmentDate());
        programEnrollment.setCompletionDate(dto.completionDate());
        programEnrollment.setStatus(dto.status());
        programEnrollment.setProgressPercentage(dto.progressPercentage());
        programEnrollment.setFinalGrade(dto.finalGrade());
        programEnrollment.setCreatedDate(dto.createdDate());
        programEnrollment.setCreatedBy(dto.createdBy());
        programEnrollment.setLastModifiedDate(dto.updatedDate());
        programEnrollment.setLastModifiedBy(dto.updatedBy());
        return programEnrollment;
    }
}