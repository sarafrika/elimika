// TrainingProgramService.java
package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.TrainingProgramDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface TrainingProgramService {
    TrainingProgramDTO createTrainingProgram(TrainingProgramDTO trainingProgramDTO);

    TrainingProgramDTO getTrainingProgramByUuid(UUID uuid);

    Page<TrainingProgramDTO> getAllTrainingPrograms(Pageable pageable);

    TrainingProgramDTO updateTrainingProgram(UUID uuid, TrainingProgramDTO trainingProgramDTO);

    void deleteTrainingProgram(UUID uuid);

    Page<TrainingProgramDTO> search(Map<String, String> searchParams, Pageable pageable);
}