package apps.sarafrika.elimika.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.otp.OneTimeCodeException;
import apps.sarafrika.elimika.shared.otp.OneTimeCodeService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.sms.SmsDelivery;
import apps.sarafrika.elimika.shared.sms.SmsSender;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import apps.sarafrika.elimika.wallet.entity.PayoutMethod;
import apps.sarafrika.elimika.wallet.entity.WalletAuditEvent;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import apps.sarafrika.elimika.wallet.enums.WalletAuditEventType;
import apps.sarafrika.elimika.wallet.repository.WalletAuditEventRepository;
import apps.sarafrika.elimika.wallet.service.PayoutMethodService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.impl.PayoutMethodServiceImpl;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Adding, verifying, defaulting and removing payout numbers, and naming organisation payout approvers. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PayoutMethodServiceImpl.class, OneTimeCodeService.class, JpaConfig.class, PayoutMethodsIntegrationTest.Sms.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@RecordApplicationEvents
@DisplayName("Payout methods")
class PayoutMethodsIntegrationTest {

    private static final Pattern DIGITS = Pattern.compile("(\\d{6})");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("otp.hmac-secret", () -> "payout-test-secret");
        registry.add("otp.resend-cooldown", () -> "PT0S");
    }

    @TestConfiguration
    static class Sms {
        static final List<String> SENT = new ArrayList<>();

        @Bean
        SmsSender smsSender() {
            return (msisdn, message) -> {
                SENT.add(msisdn + "|" + message);
                return SmsDelivery.accepted("m-" + SENT.size());
            };
        }
    }

    @Autowired
    private PayoutMethodService service;
    @Autowired
    private WalletAuditEventRepository auditRepository;
    @Autowired
    private ApplicationEvents events;

    @MockitoBean
    private DomainSecurityService domainSecurityService;

    private final UUID actor = UUID.randomUUID();
    private WalletOwner owner;

    @BeforeEach
    void setUp() {
        owner = WalletOwner.user(actor);
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(actor);
        when(domainSecurityService.getCurrentCallerContact())
                .thenReturn(Optional.of(new DomainSecurityService.CallerContact("jane@example.com", "Jane Kamau")));
    }

    @Test
    @DisplayName("a number is pending until its SMS code is entered, then verified, default and on hold")
    void addAndVerify() {
        PayoutMethodService.AddedPayoutMethod added = service.add(owner, "0712 345 678", "Personal", true);

        assertThat(added.method().getStatus()).isEqualTo(PayoutMethodStatus.PENDING_VERIFICATION);
        assertThat(added.verification().sentTo()).isEqualTo("+254 7•• ••• 678");
        assertThat(Sms.SENT.getLast()).startsWith("254712345678|");

        PayoutMethod verified = service.verify(owner, added.method().getUuid(), lastCode());

        assertThat(verified.getStatus()).isEqualTo(PayoutMethodStatus.VERIFIED);
        assertThat(verified.isDefaultMethod()).isTrue();
        assertThat(verified.getUnlockAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusHours(23));
        assertThat(verified.isUsable(OffsetDateTime.now(ZoneOffset.UTC))).isFalse();
        assertThat(events.stream(NotificationRequestedEvent.class))
                .anySatisfy(event -> {
                    assertThat(event.notificationType()).isEqualTo("SECURITY_ALERT");
                    assertThat(event.recipientEmail()).isEqualTo("jane@example.com");
                });
    }

    @Test
    @DisplayName("a wrong code leaves the number pending")
    void wrongCode() {
        PayoutMethodService.AddedPayoutMethod added = service.add(owner, "0722418903", null, true);
        String wrong = lastCode().equals("000000") ? "111111" : "000000";

        assertThatThrownBy(() -> service.verify(owner, added.method().getUuid(), wrong))
                .isInstanceOf(OneTimeCodeException.class);
        assertThat(service.list(owner)).singleElement()
                .extracting(PayoutMethod::getStatus).isEqualTo(PayoutMethodStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("the default moves when chosen, and passes on when the default is removed")
    void defaultsAndRemoval() {
        PayoutMethod first = addVerified("0712345678");
        PayoutMethod second = addVerified("0110552871");
        assertThat(second.isDefaultMethod()).isFalse();

        service.makeDefault(owner, second.getUuid());
        assertThat(service.list(owner)).first().extracting(PayoutMethod::getUuid).isEqualTo(second.getUuid());

        service.remove(owner, second.getUuid());
        assertThat(service.list(owner)).singleElement().satisfies(left -> {
            assertThat(left.getUuid()).isEqualTo(first.getUuid());
            assertThat(left.isDefaultMethod()).isTrue();
        });
    }

    @Test
    @DisplayName("bad numbers, repeats and unconfirmed ownership are refused")
    void refusals() {
        assertThatThrownBy(() -> service.add(owner, "0201234567", null, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add(owner, "0712345678", null, false)).isInstanceOf(IllegalArgumentException.class);
        service.add(owner, "0712345678", null, true);
        assertThatThrownBy(() -> service.add(owner, "+254712345678", null, true)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("each owner sees only their own numbers, and the audit trail never holds a full number")
    void scopedAndMasked() {
        addVerified("0712345678");
        WalletOwner organisation = WalletOwner.organisation(actor);

        assertThat(service.list(organisation)).isEmpty();
        List<WalletAuditEvent> trail = auditRepository.findByOwner(owner.type(), owner.uuid(), PageRequest.of(0, 20)).getContent();
        assertThat(trail).extracting(WalletAuditEvent::getEventType)
                .contains(WalletAuditEventType.PAYOUT_METHOD_ADDED, WalletAuditEventType.PAYOUT_METHOD_VERIFIED);
        assertThat(trail).allSatisfy(event -> assertThat(String.valueOf(event.getUpdatedValues()) + event.getReason())
                .doesNotContain("712345678"));
    }

    @Test
    @DisplayName("only an admin of the organisation can be named payout approver, once")
    void approvers() {
        UUID organisation = UUID.randomUUID();
        UUID admin = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        when(domainSecurityService.isOrganisationAdmin(admin, organisation)).thenReturn(true);
        when(domainSecurityService.isOrganisationAdmin(member, organisation)).thenReturn(false);

        service.addApprover(organisation, admin);
        assertThatThrownBy(() -> service.addApprover(organisation, admin)).isInstanceOf(DuplicateResourceException.class);
        assertThatThrownBy(() -> service.addApprover(organisation, member)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.approvers(organisation)).extracting(a -> a.getUserUuid()).containsExactly(admin);

        service.removeApprover(organisation, admin);
        assertThat(service.approvers(organisation)).isEmpty();
    }

    private PayoutMethod addVerified(String number) {
        PayoutMethodService.AddedPayoutMethod added = service.add(owner, number, null, true);
        return service.verify(owner, added.method().getUuid(), lastCode());
    }

    private static String lastCode() {
        Matcher matcher = DIGITS.matcher(Sms.SENT.getLast().split("\\|", 2)[1]);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
