package apps.sarafrika.elimika.shared.sms.template;

import apps.sarafrika.elimika.shared.sms.Msisdn;
import apps.sarafrika.elimika.shared.sms.SmsDelivery;
import apps.sarafrika.elimika.shared.sms.SmsSender;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The wording of every system SMS: the admin's version when saved, the built-in default otherwise. */
@Service
@RequiredArgsConstructor
public class SmsTemplateService {

    private final SmsTemplateRepository repository;
    private final SmsSender smsSender;
    private final UserLookupService userLookupService;

    /** The filled message, or empty when an admin switched this (non-security) message off. */
    @Transactional(readOnly = true)
    public Optional<String> render(SmsTemplateKey key, Map<String, String> values) {
        Optional<SmsTemplate> saved = repository.findByTemplateKey(key.name());
        if (!key.security() && saved.map(t -> !t.isEnabled()).orElse(false)) {
            return Optional.empty();
        }
        String body = saved.map(SmsTemplate::getBody)
                .filter(text -> SmsTemplateRules.problems(key, text).isEmpty())
                .orElse(key.defaultBody());
        return Optional.of(SmsTemplateRules.fill(key, body, values));
    }

    /** Security messages are always sent; this never comes back empty for them. */
    public String renderSecurity(SmsTemplateKey key, Map<String, String> values) {
        if (!key.security()) {
            throw new IllegalArgumentException(key + " is not a security message");
        }
        return render(key, values).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<SmsTemplateView> list() {
        return Arrays.stream(SmsTemplateKey.values()).map(this::view).toList();
    }

    @Transactional
    public SmsTemplateView save(String keyName, String body, boolean enabled, UUID actor) {
        SmsTemplateKey key = key(keyName);
        String text = body == null ? "" : body.trim();
        List<String> problems = SmsTemplateRules.problems(key, text);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", problems));
        }
        SmsTemplate template = repository.findByTemplateKey(key.name()).orElseGet(() -> {
            SmsTemplate created = new SmsTemplate();
            created.setTemplateKey(key.name());
            return created;
        });
        template.setBody(text);
        template.setEnabled(key.security() || enabled);
        template.setUpdatedByName(actor == null ? null : userLookupService.getUserFullName(actor).orElse(null));
        repository.save(template);
        return view(key);
    }

    @Transactional
    public SmsTemplateView reset(String keyName) {
        SmsTemplateKey key = key(keyName);
        repository.findByTemplateKey(key.name()).ifPresent(repository::delete);
        repository.flush();
        return view(key);
    }

    /** Sends the saved wording, filled with sample values, to the admin's own phone. */
    @Transactional(readOnly = true)
    public TestSend sendTest(String keyName, UUID actor) {
        SmsTemplateKey key = key(keyName);
        Optional<String> msisdn = Optional.ofNullable(actor).flatMap(userLookupService::getUserPhoneNumber)
                .flatMap(Msisdn::normalise);
        if (msisdn.isEmpty()) {
            throw new IllegalArgumentException("Add a Kenyan phone number to your profile to receive a test SMS.");
        }
        String body = repository.findByTemplateKey(key.name()).map(SmsTemplate::getBody).orElse(key.defaultBody());
        SmsDelivery delivery = smsSender.send(msisdn.get(), SmsTemplateRules.sample(key, body));
        return new TestSend(delivery.accepted(), Msisdn.mask(msisdn.get()));
    }

    private SmsTemplateView view(SmsTemplateKey key) {
        Optional<SmsTemplate> saved = repository.findByTemplateKey(key.name());
        return new SmsTemplateView(key, saved.map(SmsTemplate::getBody).orElse(key.defaultBody()),
                key.security() || saved.map(SmsTemplate::isEnabled).orElse(true), saved.isPresent(),
                saved.map(SmsTemplate::getUpdatedByName).orElse(null),
                saved.map(t -> t.getLastModifiedDate() != null ? t.getLastModifiedDate() : t.getCreatedDate()).orElse(null));
    }

    private static SmsTemplateKey key(String name) {
        try {
            return SmsTemplateKey.valueOf(name.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unknown SMS template: " + name);
        }
    }

    public record SmsTemplateView(SmsTemplateKey key, String body, boolean enabled, boolean edited,
                                  String updatedByName, java.time.LocalDateTime updatedAt) {
    }

    public record TestSend(boolean sent, String toMasked) {
    }
}
