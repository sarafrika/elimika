package apps.sarafrika.elimika.shared.utils.validation.impl;

import apps.sarafrika.elimika.shared.utils.validation.ValidUrl;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validator implementation for the @ValidUrl annotation
 * Validates that a string is a well-formed URL using modern URI class
 */
public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    @Override
    public void initialize(ValidUrl validUrl) {
        // No initialization needed for simple URL validation
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Allow null/empty values - use @NotNull/@NotBlank for required validation
        if (!StringUtils.hasText(value)) {
            return true;
        }

        try {
            URI uri = new URI(value);

            // Basic validation - ensure we have a valid scheme and host
            String scheme = uri.getScheme();
            String host = uri.getHost();

            return scheme != null && !scheme.trim().isEmpty() &&
                    host != null && !host.trim().isEmpty();

        } catch (URISyntaxException e) {
            return false;
        }
    }
}