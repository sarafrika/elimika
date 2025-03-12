package apps.sarafrika.elimika.training.service;

import apps.sarafrika.elimika.training.dto.TrainingSessionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing training sessions.
 */
public interface TrainingSessionService {

    /**
     * Creates a new training session.
     *
     * @param trainingSessionDTO The DTO containing training session details.
     * @return The created TrainingSessionDTO.
     */
    TrainingSessionDTO createTrainingSession(TrainingSessionDTO trainingSessionDTO);

    /**
     * Retrieves a training session by its UUID.
     *
     * @param uuid The UUID of the training session.
     * @return The TrainingSessionDTO representing the session.
     */
    TrainingSessionDTO getTrainingSessionByUuid(UUID uuid);

    /**
     * Retrieves all training sessions with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of TrainingSessionDTOs.
     */
    Page<TrainingSessionDTO> getAllTrainingSessions(Pageable pageable);

    /**
     * Updates an existing training session.
     *
     * @param uuid The UUID of the training session to update.
     * @param trainingSessionDTO The DTO containing updated training session details.
     * @return The updated TrainingSessionDTO.
     */
    TrainingSessionDTO updateTrainingSession(UUID uuid, TrainingSessionDTO trainingSessionDTO);

    /**
     * Deletes a training session by UUID.
     *
     * @param uuid The UUID of the training session to delete.
     */
    void deleteTrainingSession(UUID uuid);

    /**
     * Searches for training sessions based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of TrainingSessionDTOs matching the search criteria.
     */
    Page<TrainingSessionDTO> searchTrainingSessions(Map<String, String> searchParams, Pageable pageable);
}
