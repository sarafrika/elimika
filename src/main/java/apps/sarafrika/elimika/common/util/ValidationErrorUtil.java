package apps.sarafrika.elimika.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationErrorUtil {

    /**
     * Builds a validation error map from MethodArgumentNotValidException using JsonProperty names
     */
    public static Map<String, String> buildValidationErrorMap(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        BindingResult bindingResult = ex.getBindingResult();
        Object target = bindingResult.getTarget();

        for (FieldError error : bindingResult.getFieldErrors()) {
            String fieldName = getJsonPropertyName(error, target);
            errors.put(fieldName, error.getDefaultMessage());
        }
        return errors;
    }

    /**
     * Resolves the JsonProperty name for a field error, falling back to field name if not found
     */
    public static String getJsonPropertyName(FieldError error, Object target) {
        try {
            if (target != null) {
                // Handle nested field names (e.g., "address.street")
                String fieldName = error.getField();
                String[] fieldParts = fieldName.split("\\.");

                Class<?> currentClass = target.getClass();
                StringBuilder jsonPropertyPath = new StringBuilder();

                // Navigate through nested fields
                for (int i = 0; i < fieldParts.length; i++) {
                    String part = fieldParts[i];
                    Field field = findField(currentClass, part);

                    if (field != null) {
                        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                        if (jsonProperty != null && !jsonProperty.value().isEmpty()) {
                            jsonPropertyPath.append(jsonProperty.value());
                        } else {
                            jsonPropertyPath.append(part);
                        }

                        if (i < fieldParts.length - 1) {
                            jsonPropertyPath.append(".");
                            currentClass = field.getType();
                        }
                    } else {
                        jsonPropertyPath.append(part);
                        if (i < fieldParts.length - 1) {
                            jsonPropertyPath.append(".");
                        }
                    }
                }

                return jsonPropertyPath.toString();
            }
        } catch (Exception e) {
            log.debug("Could not resolve JsonProperty name for field: {}", error.getField(), e);
        }

        return error.getField();
    }

    /**
     * Finds a field in the given class or its superclasses
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && !superclass.equals(Object.class)) {
                return findField(superclass, fieldName);
            }
            return null;
        }
    }
}