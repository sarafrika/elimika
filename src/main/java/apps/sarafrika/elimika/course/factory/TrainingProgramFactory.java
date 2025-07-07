package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.TrainingProgramDTO;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TrainingProgramFactory {

    // Convert TrainingProgram entity to TrainingProgramDTO
    public static TrainingProgramDTO toDTO(TrainingProgram trainingProgram) {
        if (trainingProgram == null) {
            return null;
        }
        return new TrainingProgramDTO(
                trainingProgram.getUuid(),
                trainingProgram.getTitle(),
                trainingProgram.getInstructorUuid(),
                trainingProgram.getCategoryUuid(),
                trainingProgram.getDescription(),
                trainingProgram.getObjectives(),
                trainingProgram.getPrerequisites(),
                trainingProgram.getTotalDurationHours(),
                trainingProgram.getTotalDurationMinutes(),
                trainingProgram.getClassLimit(),
                trainingProgram.getPrice(),
                trainingProgram.getActive(),
                trainingProgram.getCreatedDate(),
                trainingProgram.getCreatedBy(),
                trainingProgram.getLastModifiedDate(),
                trainingProgram.getLastModifiedBy()
        );
    }

    // Convert TrainingProgramDTO to TrainingProgram entity
    public static TrainingProgram toEntity(TrainingProgramDTO dto) {
        if (dto == null) {
            return null;
        }
        TrainingProgram trainingProgram = new TrainingProgram();
        trainingProgram.setUuid(dto.uuid());
        trainingProgram.setTitle(dto.title());
        trainingProgram.setInstructorUuid(dto.instructorUuid());
        trainingProgram.setCategoryUuid(dto.categoryUuid());
        trainingProgram.setDescription(dto.description());
        trainingProgram.setObjectives(dto.objectives());
        trainingProgram.setPrerequisites(dto.prerequisites());
        trainingProgram.setTotalDurationHours(dto.totalDurationHours());
        trainingProgram.setTotalDurationMinutes(dto.totalDurationMinutes());
        trainingProgram.setClassLimit(dto.classLimit());
        trainingProgram.setPrice(dto.price());
        trainingProgram.setActive(dto.active());
        trainingProgram.setCreatedDate(dto.createdDate());
        trainingProgram.setCreatedBy(dto.createdBy());
        trainingProgram.setLastModifiedDate(dto.updatedDate());
        trainingProgram.setLastModifiedBy(dto.updatedBy());
        return trainingProgram;
    }
}