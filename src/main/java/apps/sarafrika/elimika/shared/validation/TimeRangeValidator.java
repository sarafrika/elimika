package apps.sarafrika.elimika.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;
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
            LocalTime startTime = getFieldValue(value, startFieldName);
            LocalTime endTime = getFieldValue(value, endFieldName);

            // If either field is null, let @NotNull handle it
            if (startTime == null || endTime == null) {
                return true;
            }

            // Validate that start time is before end time
            boolean isValid = startTime.isBefore(endTime);

            if (!isValid) {
                // Customize the error message
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        String.format("End time must be after start time. Start: %s, End: %s",
                                     startTime, endTime))
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
    private LocalTime getFieldValue(Object object, String fieldName) throws Exception {
        Class<?> clazz = object.getClass();

        try {
            // Try record-style accessor first (for records like ClassDefinitionDTO)
            Method method = clazz.getMethod(fieldName);
            Object result = method.invoke(object);
            return (LocalTime) result;
        } catch (NoSuchMethodException e) {
            // Try traditional getter style
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method method = clazz.getMethod(getterName);
            Object result = method.invoke(object);
            return (LocalTime) result;
        }
    }
}