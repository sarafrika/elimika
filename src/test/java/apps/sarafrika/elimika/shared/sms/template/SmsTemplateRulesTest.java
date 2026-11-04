package apps.sarafrika.elimika.shared.sms.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SmsTemplateRulesTest {

    @Test
    void everyDefaultPassesItsOwnChecks() {
        Arrays.stream(SmsTemplateKey.values()).forEach(key ->
                assertThat(SmsTemplateRules.problems(key, key.defaultBody())).as(key.name()).isEmpty());
    }

    @Test
    void unknownMissingAndStrayBracesAreRejected() {
        assertThat(SmsTemplateRules.problems(SmsTemplateKey.SEAT_RELEASED, "Order {order} for {amount}"))
                .anyMatch(p -> p.contains("{amount} is not available"));
        assertThat(SmsTemplateRules.problems(SmsTemplateKey.WITHDRAWAL_CODE, "Withdraw {amount} now"))
                .anyMatch(p -> p.contains("{code} must stay"));
        assertThat(SmsTemplateRules.problems(SmsTemplateKey.SEAT_RELEASED, "Order {order} {oops"))
                .anyMatch(p -> p.contains("A { or }"));
    }

    @Test
    void theWorstCaseMustFitOneSegment() {
        String body = "Elimika: " + "x".repeat(140) + " {order}{class}";
        assertThat(SmsTemplateRules.problems(SmsTemplateKey.SEAT_RELEASED, body)).anyMatch(p -> p.startsWith("Worst case"));
    }

    @Test
    void aSentenceWithAMissingOptionalValueIsLeftOut() {
        String text = SmsTemplateRules.fill(SmsTemplateKey.PAYMENT_RECEIVED, SmsTemplateKey.PAYMENT_RECEIVED.defaultBody(),
                Map.of("amount", "KES 1", "class", "Pottery", "order", "ELM-HH48B3"));
        assertThat(text).isEqualTo("Elimika: Paid KES 1 for Pottery. Order ELM-HH48B3. See you there!");
    }

    @Test
    void nonGsmCharactersAreReported() {
        assertThat(SmsTemplateRules.nonGsmCharacters("Order {order} — done ✓")).containsExactly('—', '✓');
    }
}
