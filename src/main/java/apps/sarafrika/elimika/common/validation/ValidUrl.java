package apps.sarafrika.elimika.common.validation;

import apps.sarafrika.elimika.common.validation.impl.UrlValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UrlValidator.class)
@Schema(
        type = "string",
        format = "uri",
        pattern = "^https?://[^\\s/$.?#].[^\\s]*$",
        example = "https://example.com"
)
@Documented
public @interface ValidUrl {
    String message() default "Invalid URL format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}