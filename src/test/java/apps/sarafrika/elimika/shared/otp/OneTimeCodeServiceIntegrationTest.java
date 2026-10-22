package apps.sarafrika.elimika.shared.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.sms.SmsDelivery;
import apps.sarafrika.elimika.shared.sms.SmsSender;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OneTimeCodeService.class, JpaConfig.class, OneTimeCodeServiceIntegrationTest.Sms.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("One-time codes")
class OneTimeCodeServiceIntegrationTest {

    private static final OneTimeCodePurpose PURPOSE = OneTimeCodePurpose.PAYOUT_METHOD_VERIFICATION;
    private static final Pattern DIGITS = Pattern.compile("(\\d{6})");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("otp.hmac-secret", () -> "test-secret-for-otp-hashing");
        registry.add("otp.resend-cooldown", () -> "PT0S");
    }

    @TestConfiguration
    static class Sms {
        static final List<String> SENT = new ArrayList<>();
        static boolean refuse;

        @Bean
        SmsSender smsSender() {
            return (msisdn, message) -> {
                if (refuse) {
                    return SmsDelivery.refused("1006 Invalid credentials");
                }
                SENT.add(message);
                return SmsDelivery.accepted("m-" + SENT.size());
            };
        }
    }

    @Autowired
    private OneTimeCodeService service;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID subject;

    @BeforeEach
    void setUp() {
        Sms.SENT.clear();
        Sms.refuse = false;
        subject = UUID.randomUUID();
    }

    @Test
    @DisplayName("the right code works once, and only a hash of it is stored")
    void rightCodeWorksOnce() {
        IssuedCode issued = service.issue(PURPOSE, subject, null, "254712345678", code -> "Your code is " + code);
        String code = lastCode();

        assertThat(issued.sentTo()).isEqualTo("+254 7•• ••• 678");
        assertThat(jdbc.queryForObject("select code_hash from one_time_codes where subject_uuid = ?", String.class, subject))
                .hasSize(64).doesNotContain(code);
        service.verify(PURPOSE, subject, code);
        assertThatThrownBy(() -> service.verify(PURPOSE, subject, code)).isInstanceOf(OneTimeCodeException.class);
    }

    @Test
    @DisplayName("wrong tries are counted even though they fail, and the code dies at the limit")
    void wrongTriesAreCounted() {
        service.issue(PURPOSE, subject, null, "254712345678", code -> "Code " + code);
        String code = lastCode();
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.verify(PURPOSE, subject, wrong)).isInstanceOf(OneTimeCodeException.class);
        }
        assertThat(jdbc.queryForObject("select attempts from one_time_codes where subject_uuid = ?", Integer.class, subject))
                .isEqualTo(5);
        assertThatThrownBy(() -> service.verify(PURPOSE, subject, code))
                .hasMessageContaining("expired or was replaced");
    }

    @Test
    @DisplayName("a new code replaces the old one, and a code never verifies another purpose")
    void codesAreBoundToTheirLatestIssueAndPurpose() {
        service.issue(PURPOSE, subject, null, "254712345678", code -> "Code " + code);
        String first = lastCode();
        service.issue(PURPOSE, subject, null, "254712345678", code -> "Code " + code);
        String second = lastCode();

        if (!first.equals(second)) {
            assertThatThrownBy(() -> service.verify(PURPOSE, subject, first)).isInstanceOf(OneTimeCodeException.class);
        }
        assertThatThrownBy(() -> service.verify(OneTimeCodePurpose.WITHDRAWAL_CONFIRMATION, subject, second))
                .isInstanceOf(OneTimeCodeException.class);
        service.verify(PURPOSE, subject, second);
    }

    @Test
    @DisplayName("requests are capped per hour and a refused SMS stores nothing")
    void throttledAndRefused() {
        for (int i = 0; i < 5; i++) {
            service.issue(PURPOSE, subject, null, "254712345678", code -> "Code " + code);
        }
        assertThatThrownBy(() -> service.issue(PURPOSE, subject, null, "254712345678", code -> "Code " + code))
                .isInstanceOfSatisfying(OneTimeCodeException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        UUID other = UUID.randomUUID();
        Sms.refuse = true;
        assertThatThrownBy(() -> service.issue(PURPOSE, other, null, "254712345678", code -> "Code " + code))
                .isInstanceOf(OneTimeCodeException.class);
        assertThat(jdbc.queryForObject("select count(*) from one_time_codes where subject_uuid = ?", Long.class, other))
                .isZero();
    }

    private static String lastCode() {
        Matcher matcher = DIGITS.matcher(Sms.SENT.getLast());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
