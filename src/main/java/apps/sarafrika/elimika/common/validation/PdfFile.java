package apps.sarafrika.elimika.common.validation;

import apps.sarafrika.elimika.common.validation.impl.PdfFileValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PdfFileValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PdfFile {
    String message() default "Only PDF files are allowed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
