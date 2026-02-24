package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.RubricScoringLevelDTO;
import apps.sarafrika.elimika.course.factory.RubricScoringLevelFactory;
import apps.sarafrika.elimika.course.internal.PublishedCourseVersionTriggerService;
import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import apps.sarafrika.elimika.course.repository.RubricScoringLevelRepository;
import apps.sarafrika.elimika.course.service.RubricScoringLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service implementation for managing rubric scoring levels
 * <p>
 * Provides business logic operations for managing custom scoring levels
 * within rubrics for flexible matrix configurations.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RubricScoringLevelServiceImpl implements RubricScoringLevelService {

    private final RubricScoringLevelRepository rubricScoringLevelRepository;
    private final GenericSpecificationBuilder<RubricScoringLevel> specificationBuilder;
    private final PublishedCourseVersionTriggerService publishedCourseVersionTriggerService;

    private static final String SCORING_LEVEL_NOT_FOUND_TEMPLATE = "Rubric scoring level with ID %s not found";
    private static final String SCORING_LEVEL_NOT_FOUND_IN_RUBRIC_TEMPLATE = "Rubric scoring level with ID %s not found in rubric %s";

    @Override
    public RubricScoringLevelDTO createRubricScoringLevel(UUID rubricUuid, RubricScoringLevelDTO rubricScoringLevelDTO) {
        RubricScoringLevel rubricScoringLevel = RubricScoringLevelFactory.toEntity(rubricScoringLevelDTO);
        rubricScoringLevel.setRubricUuid(rubricUuid);

        // Set next level order if not provided
        if (rubricScoringLevel.getLevelOrder() == null) {
            Integer nextOrder = rubricScoringLevelRepository.findNextLevelOrderByRubricUuid(rubricUuid);
            rubricScoringLevel.setLevelOrder(nextOrder);
        }

        RubricScoringLevel savedRubricScoringLevel = rubricScoringLevelRepository.save(rubricScoringLevel);
        publishedCourseVersionTriggerService.captureByRubricUuid(savedRubricScoringLevel.getRubricUuid());
        return RubricScoringLevelFactory.toDTO(savedRubricScoringLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public RubricScoringLevelDTO getRubricScoringLevelByUuid(UUID uuid) {
        return rubricScoringLevelRepository.findByUuid(uuid)
                .map(RubricScoringLevelFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SCORING_LEVEL_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RubricScoringLevelDTO> getScoringLevelsByRubricUuid(UUID rubricUuid) {
        return rubricScoringLevelRepository.findByRubricUuidOrderByLevelOrder(rubricUuid)
                .stream()
                .map(RubricScoringLevelFactory::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricScoringLevelDTO> getScoringLevelsByRubricUuid(UUID rubricUuid, Pageable pageable) {
        return rubricScoringLevelRepository.findByRubricUuid(rubricUuid, pageable)
                .map(RubricScoringLevelFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricScoringLevelDTO> getScoringLevelsByRubricUuidOrderByLevelOrder(UUID rubricUuid, Pageable pageable) {
        return rubricScoringLevelRepository.findByRubricUuidOrderByLevelOrder(rubricUuid, pageable)
                .map(RubricScoringLevelFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricScoringLevelDTO> getAllRubricScoringLevels(Pageable pageable) {
        return rubricScoringLevelRepository.findAll(pageable)
                .map(RubricScoringLevelFactory::toDTO);
    }

    @Override
    public RubricScoringLevelDTO updateRubricScoringLevel(UUID rubricUuid, UUID levelUuid, RubricScoringLevelDTO rubricScoringLevelDTO) {
        RubricScoringLevel existingScoringLevel = rubricScoringLevelRepository.findByUuid(levelUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SCORING_LEVEL_NOT_FOUND_TEMPLATE, levelUuid)));

        // Verify the scoring level belongs to the specified rubric
        if (!existingScoringLevel.getRubricUuid().equals(rubricUuid)) {
            throw new ResourceNotFoundException(
                    String.format(SCORING_LEVEL_NOT_FOUND_IN_RUBRIC_TEMPLATE, levelUuid, rubricUuid));
        }

        UUID previousRubricUuid = existingScoringLevel.getRubricUuid();
        updateScoringLevelFields(existingScoringLevel, rubricScoringLevelDTO);

        RubricScoringLevel updatedScoringLevel = rubricScoringLevelRepository.save(existingScoringLevel);
        publishedCourseVersionTriggerService.captureByRubricUuid(previousRubricUuid);
        publishedCourseVersionTriggerService.captureByRubricUuid(updatedScoringLevel.getRubricUuid());
        return RubricScoringLevelFactory.toDTO(updatedScoringLevel);
    }

    @Override
    public void deleteRubricScoringLevel(UUID rubricUuid, UUID levelUuid) {
        RubricScoringLevel existingScoringLevel = rubricScoringLevelRepository.findByUuid(levelUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SCORING_LEVEL_NOT_FOUND_TEMPLATE, levelUuid)));

        // Verify the scoring level belongs to the specified rubric
        if (!existingScoringLevel.getRubricUuid().equals(rubricUuid)) {
            throw new ResourceNotFoundException(
                    String.format(SCORING_LEVEL_NOT_FOUND_IN_RUBRIC_TEMPLATE, levelUuid, rubricUuid));
        }

        rubricScoringLevelRepository.deleteByUuid(levelUuid);
        publishedCourseVersionTriggerService.captureByRubricUuid(rubricUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricScoringLevelDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<RubricScoringLevel> spec = specificationBuilder.buildSpecification(
                RubricScoringLevel.class, searchParams);
        return rubricScoringLevelRepository.findAll(spec, pageable)
                .map(RubricScoringLevelFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RubricScoringLevelDTO> getPassingScoringLevels(UUID rubricUuid) {
        return rubricScoringLevelRepository.findPassingLevelsByRubricUuid(rubricUuid)
                .stream()
                .map(RubricScoringLevelFactory::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricScoringLevelDTO> getPassingScoringLevels(UUID rubricUuid, Pageable pageable) {
        return rubricScoringLevelRepository.findPassingLevelsByRubricUuid(rubricUuid, pageable)
                .map(RubricScoringLevelFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public RubricScoringLevelDTO getHighestScoringLevel(UUID rubricUuid) {
        return rubricScoringLevelRepository.findHighestLevelByRubricUuid(rubricUuid)
                .map(RubricScoringLevelFactory::toDTO)
                .orElse(null);
    }

    @Override
    public void reorderScoringLevels(UUID rubricUuid, Map<UUID, Integer> levelOrderMap) {
        for (Map.Entry<UUID, Integer> entry : levelOrderMap.entrySet()) {
            UUID levelUuid = entry.getKey();
            Integer newOrder = entry.getValue();

            RubricScoringLevel scoringLevel = rubricScoringLevelRepository.findByUuid(levelUuid)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(SCORING_LEVEL_NOT_FOUND_TEMPLATE, levelUuid)));

            // Verify the scoring level belongs to the specified rubric
            if (!scoringLevel.getRubricUuid().equals(rubricUuid)) {
                throw new ResourceNotFoundException(
                        String.format(SCORING_LEVEL_NOT_FOUND_IN_RUBRIC_TEMPLATE, levelUuid, rubricUuid));
            }

            scoringLevel.setLevelOrder(newOrder);
            rubricScoringLevelRepository.save(scoringLevel);
        }
        publishedCourseVersionTriggerService.captureByRubricUuid(rubricUuid);
    }

    @Override
    public List<RubricScoringLevelDTO> createRubricScoringLevelsBatch(UUID rubricUuid, List<RubricScoringLevelDTO> rubricScoringLevelDTOs) {
        List<RubricScoringLevel> scoringLevels = new ArrayList<>();
        
        for (RubricScoringLevelDTO dto : rubricScoringLevelDTOs) {
            RubricScoringLevel scoringLevel = RubricScoringLevelFactory.toEntity(dto);
            scoringLevel.setRubricUuid(rubricUuid);
            
            // Set next level order if not provided
            if (scoringLevel.getLevelOrder() == null) {
                Integer nextOrder = rubricScoringLevelRepository.findNextLevelOrderByRubricUuid(rubricUuid);
                scoringLevel.setLevelOrder(nextOrder);
            }
            
            scoringLevels.add(scoringLevel);
        }
        
        List<RubricScoringLevel> savedLevels = rubricScoringLevelRepository.saveAll(scoringLevels);
        publishedCourseVersionTriggerService.captureByRubricUuid(rubricUuid);
        return savedLevels.stream()
                .map(RubricScoringLevelFactory::toDTO)
                .toList();
    }

    private RubricScoringLevel createScoringLevel(UUID rubricUuid, String name, String description, 
                                                 BigDecimal points, Integer order, String colorCode, 
                                                 Boolean isPassing, String createdBy) {
        RubricScoringLevel level = new RubricScoringLevel();
        level.setRubricUuid(rubricUuid);
        level.setName(name);
        level.setDescription(description);
        level.setPoints(points);
        level.setLevelOrder(order);
        level.setColorCode(colorCode);
        level.setIsPassing(isPassing);
        level.setCreatedBy(createdBy);
        return level;
    }

    private void updateScoringLevelFields(RubricScoringLevel existingScoringLevel, RubricScoringLevelDTO dto) {
        if (dto.name() != null) {
            existingScoringLevel.setName(dto.name());
        }
        if (dto.description() != null) {
            existingScoringLevel.setDescription(dto.description());
        }
        if (dto.points() != null) {
            existingScoringLevel.setPoints(dto.points());
        }
        if (dto.levelOrder() != null) {
            existingScoringLevel.setLevelOrder(dto.levelOrder());
        }
        if (dto.colorCode() != null) {
            existingScoringLevel.setColorCode(dto.colorCode());
        }
        if (dto.isPassing() != null) {
            existingScoringLevel.setIsPassing(dto.isPassing());
        }
    }
}
