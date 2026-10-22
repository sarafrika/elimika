package apps.sarafrika.elimika.shared.sms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class MsisdnTest {

    @ParameterizedTest
    @ValueSource(strings = {"0712345678", "712345678", "254712345678", "+254 712 345 678", "+254-712-345-678"})
    void everyCommonWayOfWritingANumberBecomesOneShape(String raw) {
        assertThat(Msisdn.normalise(raw)).contains("254712345678");
    }

    @Test
    void newerOneHundredPrefixesAreMobileNumbersToo() {
        assertThat(Msisdn.normalise("0110552871")).contains("254110552871");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0201234567", "07123", "2557123456789", "07l2345678", "+44 7700 900123"})
    void landlinesForeignAndMalformedNumbersAreRefused(String raw) {
        assertThat(Msisdn.normalise(raw)).isEmpty();
    }

    @Test
    void masksAllButTheFirstDigitAndLastThree() {
        assertThat(Msisdn.mask("254712345678")).isEqualTo("+254 7•• ••• 678");
        assertThat(Msisdn.pretty("254712345678")).isEqualTo("+254 712 345 678");
    }
}
