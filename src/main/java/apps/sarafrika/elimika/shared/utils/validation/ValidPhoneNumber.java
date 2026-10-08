package apps.sarafrika.elimika.shared.utils.validation;

import apps.sarafrika.elimika.shared.utils.validation.impl.PhoneNumberValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** Accepts only E.164 numbers (e.g. {@code +254712345678}) that libphonenumber validates. */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
@Schema(
        type = "string",
        format = "phone",
        pattern = ValidPhoneNumber.E164_PATTERN,
        example = "+254712345678"
)
@Documented
public @interface ValidPhoneNumber {

    /** Documents the E.164 shape for API clients; validation itself is libphonenumber's. */
    String E164_PATTERN = "^\\+[1-9]\\d{6,14}$";

    String message() default "Phone number must be in E.164 format, e.g. +254712345678";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean mobileOnly() default false;
}
