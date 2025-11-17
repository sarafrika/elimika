package apps.sarafrika.elimika.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Validator implementation for {@link ValidTimeRange} annotation.
 * Validates that a start time is before an end time.
 */
public class TimeRangeValidator implements ConstraintValidator<ValidTimeRange, Object> {

    private String startFieldName;
    private String endFieldName;

    @Override
    public void initialize(ValidTimeRange constraintAnnotation) {
        this.startFieldName = constraintAnnotation.startField();
        this.endFieldName = constraintAnnotation.endField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Null values are handled by @NotNull
        }

        try {
            // Get the start and end time values using reflection
            Object start = getFieldValue(value, startFieldName);
            Object end = getFieldValue(value, endFieldName);

            // If either field is null, let @NotNull handle it
            if (start == null || end == null) {
                return true;
            }

            boolean isValid = isBefore(start, end);

            if (!isValid) {
                // Customize the error message
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        String.format("End time must be after start time. Start: %s, End: %s",
                                     start, end))
                       .addPropertyNode(endFieldName)
                       .addConstraintViolation();
            }

            return isValid;

        } catch (Exception e) {
            // If reflection fails, log and return false
            return false;
        }
    }

    /**
     * Gets the value of a field from an object using reflection.
     * Works with both record accessor methods and traditional getters.
     */
    private Object getFieldValue(Object object, String fieldName) throws Exception {
        Class<?> clazz = object.getClass();

        try {
            // Try record-style accessor first (for records like ClassDefinitionDTO)
            Method method = clazz.getMethod(fieldName);
            Object result = method.invoke(object);
            return result;
        } catch (NoSuchMethodException e) {
            // Try traditional getter style
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method method = clazz.getMethod(getterName);
            Object result = method.invoke(object);
            return result;
        }
    }

    /**
     * Compares supported temporal objects.
     */
    private boolean isBefore(Object start, Object end) {
        if (start instanceof LocalDateTime startDateTime && end instanceof LocalDateTime endDateTime) {
            return startDateTime.isBefore(endDateTime);
        }

        if (start instanceof LocalTime startTime && end instanceof LocalTime endTime) {
            return startTime.isBefore(endTime);
        }

        // Unsupported types – don't block validation; delegate to other constraints
        return true;
    }
}
