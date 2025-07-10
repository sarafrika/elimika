package apps.sarafrika.elimika.common.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PhoneNumberService {

    private final PhoneNumberUtil phoneNumberUtil;

    public PhoneNumberService() {
        this.phoneNumberUtil = PhoneNumberUtil.getInstance();
    }

    /**
     * Format phone number to international format (e.g., +254 712 345 678)
     */
    public String formatToInternational(String phoneNumber, String defaultCountry) {
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, defaultCountry);
            return phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
        } catch (NumberParseException e) {
            log.warn("Failed to format phone number: {} - Error: {}", phoneNumber, e.getMessage());
            return phoneNumber; // Return original if parsing fails
        }
    }

    /**
     * Format phone number to E164 format (e.g., +254712345678)
     */
    public String formatToE164(String phoneNumber, String defaultCountry) {
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, defaultCountry);
            return phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            log.warn("Failed to format phone number: {} - Error: {}", phoneNumber, e.getMessage());
            return phoneNumber; // Return original if parsing fails
        }
    }

    /**
     * Get the country code for a phone number
     */
    public String getCountryCode(String phoneNumber, String defaultCountry) {
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, defaultCountry);
            return phoneNumberUtil.getRegionCodeForNumber(parsedNumber);
        } catch (NumberParseException e) {
            log.warn("Failed to get country code for phone number: {} - Error: {}", phoneNumber, e.getMessage());
            return defaultCountry;
        }
    }

    /**
     * Check if phone number is mobile
     */
    public boolean isMobile(String phoneNumber, String defaultCountry) {
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, defaultCountry);
            PhoneNumberUtil.PhoneNumberType type = phoneNumberUtil.getNumberType(parsedNumber);
            return type == PhoneNumberUtil.PhoneNumberType.MOBILE ||
                    type == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE;
        } catch (NumberParseException e) {
            log.warn("Failed to check if phone number is mobile: {} - Error: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}