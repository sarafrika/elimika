package apps.sarafrika.elimika.common.validation.impl;

import apps.sarafrika.elimika.common.validation.ValidPhoneNumber;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    private PhoneNumberUtil phoneNumberUtil;
    private String defaultCountry;
    private boolean mobileOnly;

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.phoneNumberUtil = PhoneNumberUtil.getInstance();
        this.defaultCountry = constraintAnnotation.defaultCountry();
        this.mobileOnly = constraintAnnotation.mobileOnly();
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return true;
        }

        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, defaultCountry);

            boolean isValid = phoneNumberUtil.isValidNumber(parsedNumber);

            if (isValid && mobileOnly) {
                PhoneNumberUtil.PhoneNumberType type = phoneNumberUtil.getNumberType(parsedNumber);
                isValid = type == PhoneNumberUtil.PhoneNumberType.MOBILE ||
                        type == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE;
            }

            if (!isValid) {
                log.debug("Invalid phone number: {}", phoneNumber);
            }

            return isValid;

        } catch (NumberParseException e) {
            log.debug("Failed to parse phone number: {} - Error: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}
