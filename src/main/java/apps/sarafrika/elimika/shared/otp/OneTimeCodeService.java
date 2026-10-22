package apps.sarafrika.elimika.shared.otp;

import apps.sarafrika.elimika.shared.sms.Msisdn;
import apps.sarafrika.elimika.shared.sms.SmsDelivery;
import apps.sarafrika.elimika.shared.sms.SmsSender;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Six-digit codes sent by SMS, bound to a purpose and a subject (the payout method, the withdrawal).
 * <p>
 * Only an HMAC of the code is stored; a new code replaces the open one; a code expires, allows a few
 * tries, and works once. Requests are throttled per subject.
 */
@Slf4j
@Service
@EnableConfigurationProperties(OneTimeCodeProperties.class)
public class OneTimeCodeService {

    private static final String ALGORITHM = "HmacSHA256";

    private final OneTimeCodeRepository repository;
    private final SmsSender smsSender;
    private final OneTimeCodeProperties properties;
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    public OneTimeCodeService(OneTimeCodeRepository repository, SmsSender smsSender, OneTimeCodeProperties properties) {
        this.repository = repository;
        this.smsSender = smsSender;
        this.properties = properties;
        if (StringUtils.hasText(properties.getHmacSecret())) {
            this.key = properties.getHmacSecret().getBytes(StandardCharsets.UTF_8);
        } else {
            log.warn("otp.hmac-secret is not set; using a random key, so codes stop working after a restart");
            this.key = new byte[32];
            random.nextBytes(this.key);
        }
    }

    /**
     * Sends a new code to {@code msisdn}; {@code message} turns the code into the SMS text.
     * Any open code for the same purpose and subject stops working.
     */
    @Transactional
    public IssuedCode issue(OneTimeCodePurpose purpose, UUID subjectUuid, UUID requestedBy, String msisdn,
                            UnaryOperator<String> message) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<OneTimeCode> open = repository.findOpenForUpdate(purpose, subjectUuid);
        if (!open.isEmpty()) {
            OffsetDateTime resendAt = resendAt(open.getFirst());
            if (resendAt.isAfter(now)) {
                throw OneTimeCodeException.tooSoon(Math.max(1, Duration.between(now, resendAt).toSeconds()));
            }
        }
        if (repository.countIssuedSince(purpose, subjectUuid, LocalDateTime.now(ZoneOffset.UTC).minusHours(1))
                >= properties.getMaxPerHour()) {
            throw OneTimeCodeException.tooMany();
        }
        open.forEach(code -> code.setInvalidatedAt(now));

        String digits = String.format("%06d", random.nextInt(1_000_000));
        SmsDelivery delivery = smsSender.send(msisdn, message.apply(digits));
        if (!delivery.accepted()) {
            throw OneTimeCodeException.notSent();
        }

        OneTimeCode code = new OneTimeCode();
        code.setPurpose(purpose);
        code.setSubjectUuid(subjectUuid);
        code.setRequestedBy(requestedBy);
        code.setCodeHash(hash(purpose, subjectUuid, digits));
        code.setSentTo(Msisdn.mask(msisdn));
        code.setProviderMessageId(delivery.providerMessageId());
        code.setExpiresAt(now.plus(properties.getTtl()));
        code.setMaxAttempts(properties.getMaxAttempts());
        repository.save(code);
        return new IssuedCode(code.getSentTo(), code.getExpiresAt(), now.plus(properties.getResendCooldown()));
    }

    /**
     * Uses up the open code when {@code digits} match it; otherwise counts a wrong try and throws.
     * Runs in its own transaction so a wrong try is counted even when the caller rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = OneTimeCodeException.class)
    public void verify(OneTimeCodePurpose purpose, UUID subjectUuid, String digits) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OneTimeCode code = repository.findOpenForUpdate(purpose, subjectUuid).stream()
                .findFirst()
                .filter(open -> open.isLive(now))
                .orElseThrow(OneTimeCodeException::expired);
        String candidate = digits == null ? "" : digits.trim();
        boolean matches = candidate.matches("\\d{6}") && MessageDigest.isEqual(
                code.getCodeHash().getBytes(StandardCharsets.US_ASCII),
                hash(purpose, subjectUuid, candidate).getBytes(StandardCharsets.US_ASCII));
        if (!matches) {
            code.setAttempts(code.getAttempts() + 1);
            throw OneTimeCodeException.wrong(code.getMaxAttempts() - code.getAttempts());
        }
        code.setConsumedAt(now);
    }

    private OffsetDateTime resendAt(OneTimeCode code) {
        return code.getExpiresAt().minus(properties.getTtl()).plus(properties.getResendCooldown());
    }

    private String hash(OneTimeCodePurpose purpose, UUID subjectUuid, String digits) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] bytes = mac.doFinal((purpose.name() + ":" + subjectUuid + ":" + digits).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", ex);
        }
    }
}
