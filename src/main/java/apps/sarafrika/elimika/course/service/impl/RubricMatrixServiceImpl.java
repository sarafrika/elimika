package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.model.RubricScoring;
import apps.sarafrika.elimika.course.repository.RubricScoringRepository;
import apps.sarafrika.elimika.course.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for managing complete rubric matrices
 * <p>
 * Provides business logic operations for managing the complete rubric matrix
 * including criteria, scoring levels, and matrix cell intersections.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RubricMatrixServiceImpl implements RubricMatrixService {

    private final AssessmentRubricService assessmentRubricService;
    private final RubricCriteriaService rubricCriteriaService;
    private final RubricScoringLevelService rubricScoringLevelService;
    private final RubricScoringService rubricScoringService;
    private final RubricScoringRepository rubricScoringRepository;

    @Override
    @Transactional(readOnly = true)
    public RubricMatrixDTO getRubricMatrix(UUID rubricUuid) {
        // Get rubric details
        AssessmentRubricDTO rubric = assessmentRubricService.getAssessmentRubricByUuid(rubricUuid);
        
        // Get criteria ordered by display order
        List<RubricCriteriaDTO> criteria = rubricCriteriaService.getAllByRubricUuid(rubricUuid, null)
                .getContent()
                .stream()
                .sorted((c1, c2) -> Integer.compare(
                    c1.displayOrder() != null ? c1.displayOrder() : Integer.MAX_VALUE,
                    c2.displayOrder() != null ? c2.displayOrder() : Integer.MAX_VALUE
                ))
                .collect(Collectors.toList());

        // Get scoring levels ordered by level order
        List<RubricScoringLevelDTO> scoringLevels;
        if (Boolean.TRUE.equals(rubric.usesCustomLevels())) {
            scoringLevels = rubricScoringLevelService.getScoringLevelsByRubricUuid(rubricUuid);
        } else {
            // For global levels, we'd need to fetch from GradingLevel service
            // For now, return empty list - this would be implemented based on your GradingLevel service
            scoringLevels = List.of();
        }

        // Build matrix cells
        Map<String, RubricMatrixDTO.RubricMatrixCellDTO> matrixCells = buildMatrixCells(rubricUuid, criteria, scoringLevels);

        // Calculate statistics
        RubricMatrixDTO.MatrixStatisticsDTO statistics = calculateMatrixStatistics(criteria, scoringLevels, matrixCells, rubric);

        return new RubricMatrixDTO(rubric, scoringLevels, criteria, matrixCells, statistics);
    }

    @Override
    public RubricMatrixDTO initializeRubricMatrix(UUID rubricUuid, String template, String createdBy) {
        // Get or create scoring levels if needed
        AssessmentRubricDTO rubric = assessmentRubricService.getAssessmentRubricByUuid(rubricUuid);
        
        if (Boolean.TRUE.equals(rubric.usesCustomLevels())) {
            List<RubricScoringLevelDTO> existingLevels = rubricScoringLevelService.getScoringLevelsByRubricUuid(rubricUuid);
            
            if (existingLevels.isEmpty()) {
                // Create default scoring levels
                rubricScoringLevelService.createDefaultScoringLevels(rubricUuid, template, createdBy);
            }
        }

        return getRubricMatrix(rubricUuid);
    }

    @Override
    public RubricMatrixDTO updateMatrixCell(UUID rubricUuid, UUID criteriaUuid, UUID scoringLevelUuid, String description) {
        // Find or create the matrix cell (RubricScoring entry)
        RubricScoring matrixCell = rubricScoringRepository
                .findByCriteriaUuidAndRubricScoringLevelUuid(criteriaUuid, scoringLevelUuid)
                .orElse(null);

        if (matrixCell == null) {
            // Create new matrix cell
            RubricScoringDTO newCellDTO = new RubricScoringDTO(
                    null, criteriaUuid, scoringLevelUuid, description,
                    null, null, null, null
            );
            rubricScoringService.createRubricScoring(criteriaUuid, newCellDTO);
        } else {
            // Update existing cell
            RubricScoringDTO updatedCellDTO = new RubricScoringDTO(
                    matrixCell.getUuid(), criteriaUuid, matrixCell.getGradingLevelUuid(), 
                    description, matrixCell.getCreatedDate(), 
                    matrixCell.getCreatedBy(), matrixCell.getLastModifiedDate(), 
                    matrixCell.getLastModifiedBy()
            );
            rubricScoringService.updateRubricScoring(criteriaUuid, matrixCell.getUuid(), updatedCellDTO);
        }

        return getRubricMatrix(rubricUuid);
    }

    @Override
    public RubricMatrixDTO recalculateScores(UUID rubricUuid) {
        AssessmentRubricDTO rubric = assessmentRubricService.getAssessmentRubricByUuid(rubricUuid);
        RubricMatrixDTO matrix = getRubricMatrix(rubricUuid);

        // Calculate maximum possible score
        BigDecimal maxScore = BigDecimal.ZERO;
        BigDecimal minPassingScore = BigDecimal.ZERO;

        if (!matrix.scoringLevels().isEmpty() && !matrix.criteria().isEmpty()) {
            // Find highest scoring level
            RubricScoringLevelDTO highestLevel = matrix.scoringLevels().stream()
                    .min((l1, l2) -> Integer.compare(l1.levelOrder(), l2.levelOrder()))
                    .orElse(null);

            // Find lowest passing level
            RubricScoringLevelDTO lowestPassingLevel = matrix.scoringLevels().stream()
                    .filter(level -> Boolean.TRUE.equals(level.isPassing()))
                    .max((l1, l2) -> Integer.compare(l1.levelOrder(), l2.levelOrder()))
                    .orElse(null);

            // Equal weight calculation - all criteria are weighted equally
            if (highestLevel != null) {
                maxScore = highestLevel.points().multiply(new BigDecimal(matrix.criteria().size()));
            }
            if (lowestPassingLevel != null) {
                minPassingScore = lowestPassingLevel.points().multiply(new BigDecimal(matrix.criteria().size()));
            }
        }

        // Update rubric with calculated scores
        AssessmentRubricDTO updatedRubric = new AssessmentRubricDTO(
                rubric.uuid(), rubric.title(), rubric.description(),
                rubric.rubricType(), rubric.instructorUuid(), rubric.isPublic(),
                rubric.status(), rubric.active(), rubric.totalWeight(), rubric.weightUnit(),
                rubric.usesCustomLevels(), rubric.matrixTemplate(),
                maxScore, minPassingScore, rubric.createdDate(), rubric.createdBy(),
                rubric.updatedDate(), rubric.updatedBy()
        );

        assessmentRubricService.updateAssessmentRubric(rubricUuid, updatedRubric);

        return getRubricMatrix(rubricUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public MatrixValidationResult validateMatrix(UUID rubricUuid) {
        try {
            RubricMatrixDTO matrix = getRubricMatrix(rubricUuid);

            boolean isValid = true;
            StringBuilder messageBuilder = new StringBuilder();

            // No individual criteria weight validation needed - all criteria are equally weighted

            // Check matrix completion
            int totalCells = matrix.criteria().size() * matrix.scoringLevels().size();
            int completedCells = (int) matrix.matrixCells().values().stream()
                    .filter(RubricMatrixDTO.RubricMatrixCellDTO::isCompleted)
                    .count();
            double completionPercentage = totalCells > 0 ? (completedCells * 100.0) / totalCells : 0.0;

            if (completionPercentage < 100.0) {
                messageBuilder.append(String.format("Matrix is %.1f%% complete (%d/%d cells); ", 
                        completionPercentage, completedCells, totalCells));
            }

            // Check scores calculation
            boolean scoresCalculated = matrix.rubric().maxScore() != null && 
                    matrix.rubric().maxScore().compareTo(BigDecimal.ZERO) > 0;

            if (!scoresCalculated) {
                messageBuilder.append("Scores not calculated; ");
            }

            String finalMessage = messageBuilder.length() > 0 ? 
                    messageBuilder.toString() : "Matrix validation successful";

            return new MatrixValidationResult(
                    isValid && completionPercentage >= 80.0, // Consider 80%+ completion as valid
                    finalMessage,
                    totalCells,
                    completedCells,
                    completionPercentage,
                    isValid,
                    scoresCalculated
            );
        } catch (ResourceNotFoundException e) {
            return new MatrixValidationResult(false, "Rubric not found: " + e.getMessage(), 
                    0, 0, 0.0, false, false);
        }
    }

    private Map<String, RubricMatrixDTO.RubricMatrixCellDTO> buildMatrixCells(
            UUID rubricUuid, 
            List<RubricCriteriaDTO> criteria, 
            List<RubricScoringLevelDTO> scoringLevels) {
        
        Map<String, RubricMatrixDTO.RubricMatrixCellDTO> matrixCells = new HashMap<>();
        List<RubricScoring> existingCells = rubricScoringRepository.findMatrixCellsByRubricUuid(rubricUuid);

        // Create map for quick lookup of existing cells
        Map<String, RubricScoring> existingCellMap = existingCells.stream()
                .collect(Collectors.toMap(
                        cell -> cell.getCriteriaUuid() + "_" + 
                               (cell.getRubricScoringLevelUuid() != null ? 
                                cell.getRubricScoringLevelUuid() : cell.getGradingLevelUuid()),
                        cell -> cell
                ));

        // Build matrix cells for each criteria-level combination
        for (RubricCriteriaDTO criterion : criteria) {
            for (RubricScoringLevelDTO level : scoringLevels) {
                String cellKey = criterion.uuid() + "_" + level.uuid();
                RubricScoring existingCell = existingCellMap.get(cellKey);

                // All criteria are equally weighted, so weighted points equals raw points
                RubricMatrixDTO.RubricMatrixCellDTO cellDTO = new RubricMatrixDTO.RubricMatrixCellDTO(
                        criterion.uuid(),
                        level.uuid(),
                        existingCell != null ? existingCell.getDescription() : null,
                        level.points(),
                        level.points(), // weightedPoints same as points since criteria are equally weighted
                        existingCell != null && existingCell.getDescription() != null && 
                                !existingCell.getDescription().trim().isEmpty()
                );

                matrixCells.put(cellKey, cellDTO);
            }
        }

        return matrixCells;
    }

    private RubricMatrixDTO.MatrixStatisticsDTO calculateMatrixStatistics(
            List<RubricCriteriaDTO> criteria,
            List<RubricScoringLevelDTO> scoringLevels,
            Map<String, RubricMatrixDTO.RubricMatrixCellDTO> matrixCells,
            AssessmentRubricDTO rubric) {

        int totalCells = criteria.size() * scoringLevels.size();
        int completedCells = (int) matrixCells.values().stream()
                .filter(RubricMatrixDTO.RubricMatrixCellDTO::isCompleted)
                .count();
        double completionPercentage = totalCells > 0 ? (completedCells * 100.0) / totalCells : 0.0;

        BigDecimal maxPossibleScore = rubric.maxScore() != null ? rubric.maxScore() : BigDecimal.ZERO;
        BigDecimal weightedMaxScore = maxPossibleScore; // Same as max for now
        BigDecimal minPassingScore = rubric.minPassingScore() != null ? rubric.minPassingScore() : BigDecimal.ZERO;
        
        boolean isReadyForUse = completionPercentage >= 80.0 && 
                maxPossibleScore.compareTo(BigDecimal.ZERO) > 0;

        return new RubricMatrixDTO.MatrixStatisticsDTO(
                totalCells,
                completedCells,
                completionPercentage,
                maxPossibleScore,
                weightedMaxScore,
                minPassingScore,
                isReadyForUse
        );
    }
}