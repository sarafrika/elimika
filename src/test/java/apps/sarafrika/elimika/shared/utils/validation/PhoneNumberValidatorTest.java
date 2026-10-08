package apps.sarafrika.elimika.shared.utils.validation;

import apps.sarafrika.elimika.shared.utils.validation.impl.PhoneNumberValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberValidatorTest {

    private static class AnyPhone {
        @ValidPhoneNumber
        String value;
    }

    private static class MobilePhone {
        @ValidPhoneNumber(mobileOnly = true)
        String value;
    }

    private static PhoneNumberValidator validator(Class<?> holder) throws NoSuchFieldException {
        PhoneNumberValidator validator = new PhoneNumberValidator();
        validator.initialize(holder.getDeclaredField("value").getAnnotation(ValidPhoneNumber.class));
        return validator;
    }

    @ParameterizedTest
    @ValueSource(strings = {"+254712470083", "+254112345678", "+14155552671", "+447911123456"})
    void acceptsValidE164Mobiles(String number) throws Exception {
        assertThat(validator(MobilePhone.class).isValid(number, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0712470083", "712470083", "254712470083", "+254 712 470 083", "+254-712-470083",
            "+2547124700", "+0712470083", "00254712470083", "abc"})
    void rejectsAnythingButValidE164(String number) throws Exception {
        assertThat(validator(AnyPhone.class).isValid(number, null)).isFalse();
    }

    @Test
    void mobileOnlyRejectsLandlines() throws Exception {
        assertThat(validator(AnyPhone.class).isValid("+254202222222", null)).isTrue();
        assertThat(validator(MobilePhone.class).isValid("+254202222222", null)).isFalse();
    }

    @Test
    void leavesBlankValuesToNotBlank() throws Exception {
        assertThat(validator(AnyPhone.class).isValid(null, null)).isTrue();
        assertThat(validator(AnyPhone.class).isValid(" ", null)).isTrue();
    }
}
