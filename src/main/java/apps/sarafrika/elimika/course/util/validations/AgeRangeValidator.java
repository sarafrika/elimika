package apps.sarafrika.elimika.course.util.validations;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link ValidAgeRange} annotation.
 * <p>
 * Validates that the age lower limit is not greater than the age upper limit
 * in a CourseDTO object. The validation passes if:
 * <ul>
 *   <li>Both age limits are null</li>
 *   <li>Either age limit is null</li>
 *   <li>Lower limit is less than or equal to upper limit</li>
 * </ul>
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-06-26 11:22
 */
public class AgeRangeValidator implements ConstraintValidator<ValidAgeRange, CourseDTO> {

    /**
     * Initializes the validator.
     *
     * @param constraintAnnotation the annotation instance for a given constraint declaration
     */
    @Override
    public void initialize(ValidAgeRange constraintAnnotation) {
        // No initialization needed
    }

    /**
     * Validates the age range in the CourseDTO.
     *
     * @param courseDTO the CourseDTO object to validate
     * @param context   the validation context
     * @return true if validation passes, false otherwise
     */
    @Override
    public boolean isValid(CourseDTO courseDTO, ConstraintValidatorContext context) {
        if (courseDTO == null) {
            return true; // Let other validators handle null objects
        }

        Integer lowerLimit = courseDTO.ageLowerLimit();
        Integer upperLimit = courseDTO.ageUpperLimit();

        // If either limit is null, validation passes
        if (lowerLimit == null || upperLimit == null) {
            return true;
        }

        // Check if lower limit is less than or equal to upper limit
        boolean isValid = lowerLimit <= upperLimit;

        if (!isValid) {
            // Customize the validation message to include actual values
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format(
                            "Age lower limit (%d) must be less than or equal to age upper limit (%d)",
                            lowerLimit, upperLimit
                    )
            ).addConstraintViolation();
        }

        return isValid;
    }
}