package apps.sarafrika.elimika.shared.config;

import apps.sarafrika.elimika.shared.utils.validation.ValidPhoneNumber;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Component
public class PhoneNumberSchemaProcessor implements ModelConverter {

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;

        if (schema != null && type.getCtxAnnotations() != null) {
            // Look for @ValidPhoneNumber annotation
            ValidPhoneNumber phoneValidation = null;
            for (var annotation : type.getCtxAnnotations()) {
                if (annotation instanceof ValidPhoneNumber) {
                    phoneValidation = (ValidPhoneNumber) annotation;
                    break;
                }
            }

            if (phoneValidation != null) {
                // Add phone number specific validation to schema
                schema.setPattern(getPhonePattern(phoneValidation.defaultCountry(), phoneValidation.mobileOnly()));

                // Add description enhancement
                String currentDescription = schema.getDescription();
                String enhancedDescription = enhancePhoneDescription(currentDescription, phoneValidation);
                schema.setDescription(enhancedDescription);

                // Add example if not present
                if (schema.getExample() == null) {
                    schema.setExample(getPhoneExample(phoneValidation.defaultCountry()));
                }
            }
        }

        return schema;
    }

    private String getPhonePattern(String countryCode, boolean mobileOnly) {
        // Return appropriate regex pattern based on country
        return switch (countryCode.toUpperCase()) {
            // East African Community (EAC) Countries
            case "KE" -> mobileOnly ? "^(\\+254|0)?[17]\\d{8}$" : "^(\\+254|0)?\\d{8,9}$";
            case "UG" -> mobileOnly ? "^(\\+256|0)?[37]\\d{8}$" : "^(\\+256|0)?\\d{8,9}$";
            case "TZ" -> mobileOnly ? "^(\\+255|0)?[67]\\d{8}$" : "^(\\+255|0)?\\d{8,9}$";
            case "RW" -> mobileOnly ? "^(\\+250|0)?[78]\\d{8}$" : "^(\\+250|0)?\\d{8,9}$";
            case "BI" -> mobileOnly ? "^(\\+257|0)?[67]\\d{7}$" : "^(\\+257|0)?\\d{7,8}$";
            case "SS" -> mobileOnly ? "^(\\+211|0)?[9]\\d{8}$" : "^(\\+211|0)?\\d{8,9}$";

            // Horn of Africa
            case "ET" -> mobileOnly ? "^(\\+251|0)?[9]\\d{8}$" : "^(\\+251|0)?\\d{8,9}$";
            case "SO" -> mobileOnly ? "^(\\+252|0)?[6]\\d{8}$" : "^(\\+252|0)?\\d{8,9}$";
            case "DJ" -> mobileOnly ? "^(\\+253|0)?[67]\\d{7}$" : "^(\\+253|0)?\\d{7,8}$";
            case "ER" -> mobileOnly ? "^(\\+291|0)?[17]\\d{6}$" : "^(\\+291|0)?\\d{6,7}$";

            // Central/East Africa
            case "CD" -> mobileOnly ? "^(\\+243|0)?[89]\\d{8}$" : "^(\\+243|0)?\\d{8,9}$"; // DRC
            case "CF" -> mobileOnly ? "^(\\+236|0)?[75]\\d{7}$" : "^(\\+236|0)?\\d{7,8}$";
            case "TD" -> mobileOnly ? "^(\\+235|0)?[69]\\d{7}$" : "^(\\+235|0)?\\d{7,8}$"; // Chad
            case "CM" -> mobileOnly ? "^(\\+237|0)?[67]\\d{8}$" : "^(\\+237|0)?\\d{8,9}$"; // Cameroon

            // West Africa (Nigeria + neighbors)
            case "NG" -> mobileOnly ? "^(\\+234|0)?[789]\\d{9}$" : "^(\\+234|0)?\\d{9,10}$";
            case "GH" -> mobileOnly ? "^(\\+233|0)?[25]\\d{8}$" : "^(\\+233|0)?\\d{8,9}$";
            case "SN" -> mobileOnly ? "^(\\+221|0)?[37]\\d{8}$" : "^(\\+221|0)?\\d{8,9}$"; // Senegal
            case "ML" -> mobileOnly ? "^(\\+223|0)?[67]\\d{7}$" : "^(\\+223|0)?\\d{7,8}$"; // Mali
            case "BF" -> mobileOnly ? "^(\\+226|0)?[67]\\d{7}$" : "^(\\+226|0)?\\d{7,8}$"; // Burkina Faso
            case "NE" -> mobileOnly ? "^(\\+227|0)?[89]\\d{7}$" : "^(\\+227|0)?\\d{7,8}$"; // Niger
            case "CI" -> mobileOnly ? "^(\\+225|0)?[0145789]\\d{7}$" : "^(\\+225|0)?\\d{7,8}$"; // Ivory Coast
            case "LR" -> mobileOnly ? "^(\\+231|0)?[4578]\\d{7}$" : "^(\\+231|0)?\\d{7,8}$"; // Liberia
            case "SL" -> mobileOnly ? "^(\\+232|0)?[2378]\\d{7}$" : "^(\\+232|0)?\\d{7,8}$"; // Sierra Leone
            case "GN" -> mobileOnly ? "^(\\+224|0)?[67]\\d{7}$" : "^(\\+224|0)?\\d{7,8}$"; // Guinea
            case "GW" -> mobileOnly ? "^(\\+245|0)?[59]\\d{6}$" : "^(\\+245|0)?\\d{6,7}$"; // Guinea-Bissau
            case "GM" -> mobileOnly ? "^(\\+220|0)?[379]\\d{6}$" : "^(\\+220|0)?\\d{6,7}$"; // Gambia
            case "CV" -> mobileOnly ? "^(\\+238|0)?[59]\\d{6}$" : "^(\\+238|0)?\\d{6,7}$"; // Cape Verde
            case "TG" -> mobileOnly ? "^(\\+228|0)?[9]\\d{7}$" : "^(\\+228|0)?\\d{7,8}$"; // Togo
            case "BJ" -> mobileOnly ? "^(\\+229|0)?[5-9]\\d{7}$" : "^(\\+229|0)?\\d{7,8}$"; // Benin

            // Southern Africa (SADC)
            case "ZA" -> mobileOnly ? "^(\\+27|0)?[67]\\d{8}$" : "^(\\+27|0)?\\d{8,9}$";
            case "ZW" -> mobileOnly ? "^(\\+263|0)?7\\d{8}$" : "^(\\+263|0)?\\d{8,9}$";
            case "ZM" -> mobileOnly ? "^(\\+260|0)?[79]\\d{8}$" : "^(\\+260|0)?\\d{8,9}$";
            case "MW" -> mobileOnly ? "^(\\+265|0)?[89]\\d{7}$" : "^(\\+265|0)?\\d{7,8}$";
            case "MZ" -> mobileOnly ? "^(\\+258|0)?[8]\\d{8}$" : "^(\\+258|0)?\\d{8,9}$";
            case "BW" -> mobileOnly ? "^(\\+267|0)?[67]\\d{7}$" : "^(\\+267|0)?\\d{7,8}$";
            case "NA" -> mobileOnly ? "^(\\+264|0)?[68]\\d{7}$" : "^(\\+264|0)?\\d{7,8}$";
            case "SZ" -> mobileOnly ? "^(\\+268|0)?[67]\\d{7}$" : "^(\\+268|0)?\\d{7,8}$"; // Eswatini
            case "LS" -> mobileOnly ? "^(\\+266|0)?[56]\\d{7}$" : "^(\\+266|0)?\\d{7,8}$"; // Lesotho
            case "AO" -> mobileOnly ? "^(\\+244|0)?9\\d{8}$" : "^(\\+244|0)?\\d{8,9}$"; // Angola

            // North Africa
            case "EG" -> mobileOnly ? "^(\\+20|0)?1[0125]\\d{8}$" : "^(\\+20|0)?\\d{8,10}$";
            case "LY" -> mobileOnly ? "^(\\+218|0)?9\\d{8}$" : "^(\\+218|0)?\\d{8,9}$";
            case "TN" -> mobileOnly ? "^(\\+216|0)?[2459]\\d{7}$" : "^(\\+216|0)?\\d{7,8}$";
            case "DZ" -> mobileOnly ? "^(\\+213|0)?[567]\\d{8}$" : "^(\\+213|0)?\\d{8,9}$"; // Algeria
            case "MA" -> mobileOnly ? "^(\\+212|0)?[67]\\d{8}$" : "^(\\+212|0)?\\d{8,9}$"; // Morocco
            case "SD" -> mobileOnly ? "^(\\+249|0)?9\\d{8}$" : "^(\\+249|0)?\\d{8,9}$"; // Sudan

            // Island Nations
            case "MG" -> mobileOnly ? "^(\\+261|0)?3\\d{8}$" : "^(\\+261|0)?\\d{8,9}$"; // Madagascar
            case "MU" -> mobileOnly ? "^(\\+230|0)?[59]\\d{7}$" : "^(\\+230|0)?\\d{7,8}$"; // Mauritius
            case "SC" -> mobileOnly ? "^(\\+248|0)?[25]\\d{6}$" : "^(\\+248|0)?\\d{6,7}$"; // Seychelles
            case "KM" -> mobileOnly ? "^(\\+269|0)?[37]\\d{6}$" : "^(\\+269|0)?\\d{6,7}$"; // Comoros
            case "ST" -> mobileOnly ? "^(\\+239|0)?9\\d{6}$" : "^(\\+239|0)?\\d{6,7}$"; // São Tomé and Príncipe

            // Other International
            case "US", "CA" -> "^(\\+1)?[2-9]\\d{9}$";
            case "UK", "GB" -> "^(\\+44|0)?[1-9]\\d{8,10}$";
            case "IN" -> mobileOnly ? "^(\\+91|0)?[6-9]\\d{9}$" : "^(\\+91|0)?\\d{10,11}$";
            case "CN" -> mobileOnly ? "^(\\+86|0)?1[3-9]\\d{9}$" : "^(\\+86|0)?\\d{10,11}$";

            default -> "^\\+?[1-9]\\d{7,14}$"; // Generic international format
        };
    }

    private String getPhoneExample(String countryCode) {
        return switch (countryCode.toUpperCase()) {
            // East African Community (EAC) Countries
            case "KE" -> "+254712345678";
            case "UG" -> "+256712345678";
            case "TZ" -> "+255612345678";
            case "RW" -> "+250781234567";
            case "BI" -> "+25761234567";
            case "SS" -> "+211912345678";

            // Horn of Africa
            case "ET" -> "+251912345678";
            case "SO" -> "+252612345678";
            case "DJ" -> "+25361234567";
            case "ER" -> "+2911234567";

            // Central/East Africa
            case "CD" -> "+243812345678"; // DRC
            case "CF" -> "+23675123456";
            case "TD" -> "+23569123456"; // Chad
            case "CM" -> "+237671234567"; // Cameroon

            // West Africa (Nigeria + neighbors)
            case "NG" -> "+2348012345678";
            case "GH" -> "+233241234567";
            case "SN" -> "+221771234567"; // Senegal
            case "ML" -> "+22361234567"; // Mali
            case "BF" -> "+22661234567"; // Burkina Faso
            case "NE" -> "+22789123456"; // Niger
            case "CI" -> "+22507123456"; // Ivory Coast
            case "LR" -> "+23145123456"; // Liberia
            case "SL" -> "+23230123456"; // Sierra Leone
            case "GN" -> "+22461234567"; // Guinea
            case "GW" -> "+2459123456"; // Guinea-Bissau
            case "GM" -> "+2203123456"; // Gambia
            case "CV" -> "+2389123456"; // Cape Verde
            case "TG" -> "+22891234567"; // Togo
            case "BJ" -> "+22950123456"; // Benin

            // Southern Africa (SADC)
            case "ZA" -> "+27821234567";
            case "ZW" -> "+263712345678";
            case "ZM" -> "+260971234567";
            case "MW" -> "+265881234567";
            case "MZ" -> "+258821234567";
            case "BW" -> "+26771234567";
            case "NA" -> "+264811234567";
            case "SZ" -> "+26876123456"; // Eswatini
            case "LS" -> "+26650123456"; // Lesotho
            case "AO" -> "+244912345678"; // Angola

            // North Africa
            case "EG" -> "+201012345678";
            case "LY" -> "+218912345678";
            case "TN" -> "+21620123456";
            case "DZ" -> "+213551234567"; // Algeria
            case "MA" -> "+212661234567"; // Morocco
            case "SD" -> "+249912345678"; // Sudan

            // Island Nations
            case "MG" -> "+261321234567"; // Madagascar
            case "MU" -> "+230512345678"; // Mauritius
            case "SC" -> "+2482512345"; // Seychelles
            case "KM" -> "+2693123456"; // Comoros
            case "ST" -> "+2399123456"; // São Tomé and Príncipe

            // Other International
            case "US", "CA" -> "+12345678901";
            case "UK", "GB" -> "+447123456789";
            case "IN" -> "+919876543210";
            case "CN" -> "+8613812345678";

            default -> "+1234567890";
        };
    }

    private String enhancePhoneDescription(String currentDescription, ValidPhoneNumber validation) {
        StringBuilder enhanced = new StringBuilder();

        if (currentDescription != null && !currentDescription.trim().isEmpty()) {
            enhanced.append(currentDescription).append("\n\n");
        }

        enhanced.append("**Phone Number Validation:**\n");
        enhanced.append("- Country: ").append(validation.defaultCountry()).append("\n");
        enhanced.append("- Type: ").append(validation.mobileOnly() ? "Mobile only" : "Any phone type").append("\n");
        enhanced.append("- Format: International (+country code) or local format supported");

        return enhanced.toString();
    }
}