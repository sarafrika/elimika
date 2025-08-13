package apps.sarafrika.elimika.course.util.validations;

import apps.sarafrika.elimika.course.dto.RubricCriteriaDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Utility class for validating rubric weight distributions and calculations
 * <p>
 * Provides comprehensive weight validation for assessment rubrics ensuring that:
 * - Individual criteria weights are within acceptable ranges
 * - Total weight distribution sums correctly for the rubric
 * - Weight calculations maintain precision for scoring accuracy
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RubricWeightValidator {

    /**
     * Default tolerance for weight distribution validation (0.01 for percentage-based systems)
     */
    public static final BigDecimal DEFAULT_WEIGHT_TOLERANCE = new BigDecimal("0.01");
    
    /**
     * Maximum allowed weight for a single criteria (100.00 for percentage-based systems)
     */
    public static final BigDecimal MAX_CRITERIA_WEIGHT = new BigDecimal("100.00");
    
    /**
     * Minimum allowed weight for a single criteria (0.00 for percentage-based systems)
     */
    public static final BigDecimal MIN_CRITERIA_WEIGHT = new BigDecimal("0.00");

    /**
     * Validates that the total weight of all criteria matches the expected total
     * within an acceptable tolerance range.
     *
     * @param criteria List of rubric criteria to validate
     * @param expectedTotal Expected total weight (typically 100.00 for percentage)
     * @param tolerance Acceptable variance from expected total
     * @return ValidationResult containing validation status and details
     */
    public static ValidationResult validateTotalWeight(List<RubricCriteriaDTO> criteria, 
                                                      BigDecimal expectedTotal, 
                                                      BigDecimal tolerance) {
        if (CollectionUtils.isEmpty(criteria)) {
            return ValidationResult.failure("No criteria provided for weight validation");
        }
        
        if (expectedTotal == null || expectedTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.failure("Expected total weight must be greater than zero");
        }
        
        if (tolerance == null || tolerance.compareTo(BigDecimal.ZERO) < 0) {
            tolerance = DEFAULT_WEIGHT_TOLERANCE;
        }

        BigDecimal totalWeight = calculateTotalWeight(criteria);
        BigDecimal difference = totalWeight.subtract(expectedTotal).abs();
        
        if (difference.compareTo(tolerance) > 0) {
            return ValidationResult.failure(
                String.format("Weight distribution invalid. Total: %s, Expected: %s, Difference: %s exceeds tolerance: %s",
                    totalWeight, expectedTotal, difference, tolerance)
            );
        }
        
        return ValidationResult.success("Weight distribution is valid");
    }

    /**
     * Validates that the total weight of all criteria matches the expected total
     * using the default tolerance.
     *
     * @param criteria List of rubric criteria to validate
     * @param expectedTotal Expected total weight
     * @return ValidationResult containing validation status and details
     */
    public static ValidationResult validateTotalWeight(List<RubricCriteriaDTO> criteria, 
                                                      BigDecimal expectedTotal) {
        return validateTotalWeight(criteria, expectedTotal, DEFAULT_WEIGHT_TOLERANCE);
    }

    /**
     * Validates individual criteria weights ensuring they fall within acceptable ranges
     * and are properly formatted for calculation precision.
     *
     * @param criteria List of rubric criteria to validate
     * @return ValidationResult containing validation status and details
     */
    public static ValidationResult validateIndividualWeights(List<RubricCriteriaDTO> criteria) {
        if (CollectionUtils.isEmpty(criteria)) {
            return ValidationResult.failure("No criteria provided for individual weight validation");
        }

        for (int i = 0; i < criteria.size(); i++) {
            RubricCriteriaDTO criterion = criteria.get(i);
            BigDecimal weight = criterion.weight();
            
            if (weight == null) {
                return ValidationResult.failure(
                    String.format("Criteria %d ('%s') has null weight", i + 1, criterion.componentName())
                );
            }
            
            if (weight.compareTo(MIN_CRITERIA_WEIGHT) < 0) {
                return ValidationResult.failure(
                    String.format("Criteria %d ('%s') weight %s is below minimum %s", 
                        i + 1, criterion.componentName(), weight, MIN_CRITERIA_WEIGHT)
                );
            }
            
            if (weight.compareTo(MAX_CRITERIA_WEIGHT) > 0) {
                return ValidationResult.failure(
                    String.format("Criteria %d ('%s') weight %s exceeds maximum %s", 
                        i + 1, criterion.componentName(), weight, MAX_CRITERIA_WEIGHT)
                );
            }
            
            // Check for excessive decimal places (should be 2 decimal places max for percentage)
            if (weight.scale() > 2) {
                return ValidationResult.failure(
                    String.format("Criteria %d ('%s') weight %s has too many decimal places (max 2)", 
                        i + 1, criterion.componentName(), weight)
                );
            }
        }
        
        return ValidationResult.success("All individual criteria weights are valid");
    }

    /**
     * Performs comprehensive validation of rubric weight distribution including
     * individual weight validation and total weight validation.
     *
     * @param criteria List of rubric criteria to validate
     * @param expectedTotal Expected total weight distribution
     * @param tolerance Acceptable variance from expected total
     * @return ValidationResult containing validation status and details
     */
    public static ValidationResult validateRubricWeights(List<RubricCriteriaDTO> criteria, 
                                                        BigDecimal expectedTotal, 
                                                        BigDecimal tolerance) {
        // First validate individual weights
        ValidationResult individualResult = validateIndividualWeights(criteria);
        if (!individualResult.isValid()) {
            return individualResult;
        }
        
        // Then validate total weight distribution
        ValidationResult totalResult = validateTotalWeight(criteria, expectedTotal, tolerance);
        if (!totalResult.isValid()) {
            return totalResult;
        }
        
        return ValidationResult.success(
            String.format("Rubric weights are valid. Total: %s matches expected: %s within tolerance: %s", 
                calculateTotalWeight(criteria), expectedTotal, tolerance)
        );
    }

    /**
     * Performs comprehensive validation using default tolerance.
     *
     * @param criteria List of rubric criteria to validate
     * @param expectedTotal Expected total weight distribution
     * @return ValidationResult containing validation status and details
     */
    public static ValidationResult validateRubricWeights(List<RubricCriteriaDTO> criteria, 
                                                        BigDecimal expectedTotal) {
        return validateRubricWeights(criteria, expectedTotal, DEFAULT_WEIGHT_TOLERANCE);
    }

    /**
     * Calculates the total weight from all criteria in the list.
     *
     * @param criteria List of rubric criteria
     * @return Total weight as BigDecimal with proper precision
     */
    public static BigDecimal calculateTotalWeight(List<RubricCriteriaDTO> criteria) {
        if (CollectionUtils.isEmpty(criteria)) {
            return BigDecimal.ZERO;
        }
        
        return criteria.stream()
                .map(RubricCriteriaDTO::weight)
                .filter(weight -> weight != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Suggests equal weight distribution for a given number of criteria
     * and total weight target.
     *
     * @param criteriaCount Number of criteria to distribute weight across
     * @param totalWeight Total weight to distribute
     * @return Suggested weight per criteria
     */
    public static BigDecimal suggestEqualWeight(int criteriaCount, BigDecimal totalWeight) {
        if (criteriaCount <= 0 || totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return totalWeight.divide(
            new BigDecimal(criteriaCount), 
            2, 
            RoundingMode.HALF_UP
        );
    }

    /**
     * Validation result container for weight validation operations
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success(String message) {
            return new ValidationResult(true, message);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, message='%s'}", valid, message);
        }
    }
}