package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.RubricMatrixCellDTO;
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
     * Updates a matrix cell description.
     *
     * @param rubricUuid the UUID of the rubric
     * @param cellUpdate the matrix cell update data
     * @return updated rubric matrix
     */
    RubricMatrixDTO updateMatrixCell(UUID rubricUuid, RubricMatrixCellDTO cellUpdate);

    /**
     * Calculates and updates the maximum possible scores for the rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return updated rubric matrix with calculated scores
     */
    RubricMatrixDTO recalculateScores(UUID rubricUuid);

    /**
     * Auto-generates empty matrix cells when both criteria and scoring levels exist.
     * This creates the matrix structure without descriptions for user to fill.
     *
     * @param rubricUuid the UUID of the rubric
     */
    void autoGenerateMatrixCells(UUID rubricUuid);

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