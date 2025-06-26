package apps.sarafrika.elimika.course.util.validations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure that age lower limit is not greater than age upper limit.
 * <p>
 * This annotation validates that if both age limits are provided, the lower limit must be
 * less than or equal to the upper limit. If either limit is null, validation passes.
 *
 * @author Sarafrika Team
 * @version 1.0
 * @since 2024-01-01
 */
@Documented
@Constraint(validatedBy = AgeRangeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAgeRange {

    /**
     * Default validation error message.
     *
     * @return the error message template
     */
    String message() default "Age lower limit must be less than or equal to age upper limit";

    /**
     * Validation groups.
     *
     * @return array of validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload for validation metadata.
     *
     * @return array of payload classes
     */
    Class<? extends Payload>[] payload() default {};
}
