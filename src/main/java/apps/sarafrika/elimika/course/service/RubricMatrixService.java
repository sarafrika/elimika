package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.RubricMatrixDTO;

import java.util.UUID;

/**
 * Service interface for managing complete rubric matrices
 * <p>
 * Provides business logic operations for managing the complete rubric matrix
 * including criteria, scoring levels, and matrix cell intersections.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
public interface RubricMatrixService {

    /**
     * Retrieves the complete rubric matrix including all components.
     *
     * @param rubricUuid the UUID of the rubric
     * @return complete rubric matrix with statistics
     */
    RubricMatrixDTO getRubricMatrix(UUID rubricUuid);

    /**
     * Initializes a rubric matrix with default structure.
     *
     * @param rubricUuid the UUID of the rubric
     * @param template the template to use for scoring levels
     * @param createdBy the user initializing the matrix
     * @return initialized rubric matrix
     */
    RubricMatrixDTO initializeRubricMatrix(UUID rubricUuid, String template, String createdBy);

    /**
     * Updates a matrix cell description.
     *
     * @param rubricUuid the UUID of the rubric
     * @param criteriaUuid the UUID of the criteria
     * @param scoringLevelUuid the UUID of the scoring level
     * @param description the new description
     * @return updated rubric matrix
     */
    RubricMatrixDTO updateMatrixCell(UUID rubricUuid, UUID criteriaUuid, UUID scoringLevelUuid, String description);

    /**
     * Calculates and updates the maximum possible scores for the rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return updated rubric matrix with calculated scores
     */
    RubricMatrixDTO recalculateScores(UUID rubricUuid);

    /**
     * Validates the matrix completeness and consistency.
     *
     * @param rubricUuid the UUID of the rubric
     * @return validation results with any issues found
     */
    MatrixValidationResult validateMatrix(UUID rubricUuid);

    /**
     * Matrix validation result container
     */
    record MatrixValidationResult(
        boolean isValid,
        String message,
        int totalCells,
        int completedCells,
        double completionPercentage,
        boolean weightsValid,
        boolean scoresCalculated
    ) {}
}