package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.AssignmentDTO;
import apps.sarafrika.elimika.course.model.Assignment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssignmentFactory {

    // Convert Assignment entity to AssignmentDTO
    public static AssignmentDTO toDTO(Assignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new AssignmentDTO(
                assignment.getUuid(),
                assignment.getLessonUuid(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getInstructions(),
                assignment.getDueDate(),
                assignment.getMaxPoints(),
                assignment.getRubricUuid(),
                assignment.getSubmissionTypes(),
                assignment.getPublished(),
                assignment.getCreatedDate(),
                assignment.getCreatedBy(),
                assignment.getLastModifiedDate(),
                assignment.getLastModifiedBy()
        );
    }

    // Convert AssignmentDTO to Assignment entity
    public static Assignment toEntity(AssignmentDTO dto) {
        if (dto == null) {
            return null;
        }
        Assignment assignment = new Assignment();
        assignment.setUuid(dto.uuid());
        assignment.setLessonUuid(dto.lessonUuid());
        assignment.setTitle(dto.title());
        assignment.setDescription(dto.description());
        assignment.setInstructions(dto.instructions());
        assignment.setDueDate(dto.dueDate());
        assignment.setMaxPoints(dto.maxPoints());
        assignment.setRubricUuid(dto.rubricUuid());
        assignment.setSubmissionTypes(dto.submissionTypes());
        assignment.setPublished(dto.published());
        assignment.setCreatedDate(dto.createdDate());
        assignment.setCreatedBy(dto.createdBy());
        assignment.setLastModifiedDate(dto.updatedDate());
        assignment.setLastModifiedBy(dto.updatedBy());
        return assignment;
    }
}