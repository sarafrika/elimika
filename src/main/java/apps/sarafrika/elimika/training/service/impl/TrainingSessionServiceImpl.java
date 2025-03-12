package apps.sarafrika.elimika.training.service.impl;

import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.training.dto.TrainingSessionDTO;
import apps.sarafrika.elimika.training.factory.TrainingSessionFactory;
import apps.sarafrika.elimika.training.model.TrainingSession;
import apps.sarafrika.elimika.training.repository.TrainingSessionRepository;
import apps.sarafrika.elimika.training.service.TrainingSessionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Implementation of the {@link TrainingSessionService} interface.
 */
@Service
@RequiredArgsConstructor
public class TrainingSessionServiceImpl implements TrainingSessionService {
    private static final String TRAINING_SESSION_NOT_FOUND = "Training with id %s session not found.";

    private final TrainingSessionRepository trainingSessionRepository;
    private final GenericSpecificationBuilder<TrainingSession> specificationBuilder;

    /**
     * Creates a new training session.
     *
     * @param trainingSessionDTO The DTO containing training session details.
     * @return The created TrainingSessionDTO.
     */
    @Override
    public TrainingSessionDTO createTrainingSession(TrainingSessionDTO trainingSessionDTO) {
        TrainingSession trainingSession = TrainingSessionFactory.toEntity(trainingSessionDTO);
        return TrainingSessionFactory.toDTO(trainingSessionRepository.save(trainingSession));
    }

    /**
     * Retrieves a training session by its UUID.
     *
     * @param uuid The UUID of the training session.
     * @return The TrainingSessionDTO representing the session.
     * @throws RecordNotFoundException If no training session is found with the given UUID.
     */
    @Override
    public TrainingSessionDTO getTrainingSessionByUuid(UUID uuid) {
        return TrainingSessionFactory.toDTO(findTrainingSessionByUuid(uuid));
    }

    /**
     * Retrieves all training sessions with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of TrainingSessionDTOs.
     */
    @Override
    public Page<TrainingSessionDTO> getAllTrainingSessions(Pageable pageable) {
        return trainingSessionRepository.findAll(pageable)
                .map(TrainingSessionFactory::toDTO);
    }

    /**
     * Updates an existing training session.
     *
     * @param uuid The UUID of the training session to update.
     * @param dto The DTO containing updated training session details.
     * @return The updated TrainingSessionDTO.
     * @throws RecordNotFoundException If no training session is found with the given UUID.
     */
    @Override
    public TrainingSessionDTO updateTrainingSession(UUID uuid, TrainingSessionDTO dto) {
        TrainingSession session = findTrainingSessionByUuid(uuid);
        updateSessionFields(session, dto);
        return TrainingSessionFactory.toDTO(trainingSessionRepository.save(session));
    }

    /**
     * Deletes a training session by UUID.
     *
     * @param uuid The UUID of the training session to delete.
     * @throws EntityNotFoundException If no training session is found with the given UUID.
     */
    @Override
    public void deleteTrainingSession(UUID uuid) {
        TrainingSession session = trainingSessionRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Training session not found with UUID: " + uuid));
        trainingSessionRepository.delete(session);
    }

    /**
     * Searches for training sessions based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of TrainingSessionDTOs matching the search criteria.
     */
    @Override
    public Page<TrainingSessionDTO> searchTrainingSessions(Map<String, String> searchParams, Pageable pageable) {
        Specification<TrainingSession> specification =
                specificationBuilder.buildSpecification(TrainingSession.class, searchParams);

        return trainingSessionRepository.findAll(specification, pageable)
                .map(TrainingSessionFactory::toDTO);
    }

    /**
     * Finds a training session by UUID.
     *
     * @param uuid The UUID of the training session.
     * @return The TrainingSession entity.
     * @throws RecordNotFoundException If no training session is found with the given UUID.
     */
    private TrainingSession findTrainingSessionByUuid(UUID uuid) {
        return trainingSessionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RecordNotFoundException(String.format(TRAINING_SESSION_NOT_FOUND, uuid)));
    }

    /**
     * Updates the fields of a training session entity from the DTO.
     *
     * @param session The existing training session entity to update.
     * @param dto The DTO containing the new values.
     */
    private void updateSessionFields(TrainingSession session, TrainingSessionDTO dto) {
        session.setCourseUuid(dto.courseUuid());
        session.setTraineruuid(dto.trainerUuid());
        session.setStartDate(dto.startDate());
        session.setEndDate(dto.endDate());
        session.setClassMode(TrainingSession.ClassMode.valueOf(dto.classMode()));
        session.setLocation(dto.location());
        session.setMeetingLink(dto.meetingLink());
        session.setSchedule(dto.schedule());
        session.setCapacityLimit(dto.capacityLimit());
        session.setCurrentEnrollmentCount(dto.currentEnrollmentCount());
        session.setWaitingListCount(dto.waitingListCount());
        session.setGroupOrIndividual(TrainingSession.GroupType.valueOf(dto.groupOrIndividual()));
    }
}