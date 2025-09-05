package apps.sarafrika.elimika.shared.utils.validation;

import apps.sarafrika.elimika.shared.utils.validation.impl.PhoneNumberValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
@Schema(
        type = "string",
        format = "phone",
        pattern = "^(\\+254|0)?[17]\\d{8}$",
        example = "+254712345678"
)
@Documented
public @interface ValidPhoneNumber {
    String message() default "Invalid phone number format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String defaultCountry() default "KE";
    boolean mobileOnly() default false;
}