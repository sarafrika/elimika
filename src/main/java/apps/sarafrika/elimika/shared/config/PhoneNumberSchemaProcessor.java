package apps.sarafrika.elimika.shared.config;

import apps.sarafrika.elimika.shared.utils.validation.ValidPhoneNumber;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/** Documents {@link ValidPhoneNumber} fields as E.164 in the OpenAPI schema. */
@Component
public class PhoneNumberSchemaProcessor implements ModelConverter {

    private static final String EXAMPLE = "+254712345678";

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        if (schema == null || type.getCtxAnnotations() == null) {
            return schema;
        }
        for (var annotation : type.getCtxAnnotations()) {
            if (annotation instanceof ValidPhoneNumber phoneValidation) {
                schema.setPattern(ValidPhoneNumber.E164_PATTERN);
                schema.setDescription(describe(schema.getDescription(), phoneValidation));
                if (schema.getExample() == null) {
                    schema.setExample(EXAMPLE);
                }
                break;
            }
        }
        return schema;
    }

    private String describe(String currentDescription, ValidPhoneNumber validation) {
        String prefix = currentDescription == null || currentDescription.isBlank() ? "" : currentDescription + "\n\n";
        return prefix + "**Phone Number Validation:**\n"
                + "- Type: " + (validation.mobileOnly() ? "Mobile only" : "Any phone type") + "\n"
                + "- Format: E.164 only (+ then country code and number, no spaces), e.g. " + EXAMPLE;
    }
}
