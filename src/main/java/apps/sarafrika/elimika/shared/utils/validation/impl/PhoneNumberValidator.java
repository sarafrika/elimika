package apps.sarafrika.elimika.shared.utils.validation.impl;

import apps.sarafrika.elimika.shared.utils.validation.ValidPhoneNumber;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Valid when libphonenumber accepts the number and its E.164 form is exactly what was sent. */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    // "ZZ" is libphonenumber's unknown region, so only numbers with a leading + parse.
    private static final String NO_DEFAULT_REGION = "ZZ";
    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();

    private boolean mobileOnly;

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.mobileOnly = constraintAnnotation.mobileOnly();
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return true;
        }
        try {
            Phonenumber.PhoneNumber parsed = UTIL.parse(phoneNumber, NO_DEFAULT_REGION);
            if (!UTIL.isValidNumber(parsed)
                    || !phoneNumber.equals(UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164))) {
                return false;
            }
            if (!mobileOnly) {
                return true;
            }
            PhoneNumberUtil.PhoneNumberType type = UTIL.getNumberType(parsed);
            return type == PhoneNumberUtil.PhoneNumberType.MOBILE
                    || type == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE;
        } catch (NumberParseException e) {
            return false;
        }
    }
}
