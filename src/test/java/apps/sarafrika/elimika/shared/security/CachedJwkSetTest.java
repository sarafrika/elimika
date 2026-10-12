package apps.sarafrika.elimika.shared.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachedJwkSetTest {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Duration MIN_REFRESH = Duration.ofSeconds(30);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-12T08:00:00Z"));
    private final AtomicInteger loads = new AtomicInteger();
    private final AtomicReference<Callable<JWKSet>> source = new AtomicReference<>();

    private OctetKeyPair key;
    private CachedJwkSet cache;

    @BeforeEach
    void setUp() throws Exception {
        key = new OctetKeyPairGenerator(Curve.Ed25519).keyID("kid-1").generate();
        source.set(() -> new JWKSet(key.toPublicJWK()));
        cache = new CachedJwkSet(() -> {
            loads.incrementAndGet();
            return source.get().call();
        }, TTL, MIN_REFRESH, clock);
    }

    @Test
    void decoderVerifiesManyTokensWithOneJwksFetch() throws Exception {
        JwtDecoder decoder = new JwtConfig(cache).jwtDecoder();

        for (int i = 0; i < 3; i++) {
            assertThat(decoder.decode(sign(key, "user-" + i)).getSubject()).isEqualTo("user-" + i);
        }

        assertThat(loads).hasValue(1);
    }

    @Test
    void warmUpLoadsBeforeTheFirstLookup() {
        cache.warmUp();
        cache.findByKeyId("kid-1");

        assertThat(loads).hasValue(1);
    }

    @Test
    void reloadsOnceTheTtlHasPassed() {
        cache.findByKeyId("kid-1");
        clock.advance(TTL.plusSeconds(1));
        cache.findByKeyId("kid-1");
        cache.findByKeyId("kid-1");

        assertThat(loads).hasValue(2);
    }

    @Test
    void unknownKeyIdRefreshesForRotationButIsRateLimited() throws Exception {
        cache.findByKeyId("kid-1");
        OctetKeyPair rotated = new OctetKeyPairGenerator(Curve.Ed25519).keyID("kid-2").generate();
        source.set(() -> new JWKSet(rotated.toPublicJWK()));

        assertThatThrownBy(() -> cache.findByKeyId("kid-2")).isInstanceOf(JwtException.class);
        assertThat(loads).hasValue(1);

        clock.advance(MIN_REFRESH.plusSeconds(1));
        assertThat(cache.findByKeyId("kid-2").getKeyID()).isEqualTo("kid-2");
        assertThatThrownBy(() -> cache.findByKeyId("forged")).isInstanceOf(JwtException.class);
        assertThat(loads).hasValue(2);
    }

    @Test
    void keepsServingTheLastKeysWhenARefreshFails() {
        cache.findByKeyId("kid-1");
        source.set(() -> {
            throw new IOException("keycloak down");
        });
        clock.advance(TTL.plusSeconds(1));

        assertThat(cache.findByKeyId("kid-1").getKeyID()).isEqualTo("kid-1");
        assertThat(cache.findByKeyId("kid-1").getKeyID()).isEqualTo("kid-1");
        assertThat(loads).hasValue(2);
    }

    @Test
    void failsWhenTheFirstLoadFails() {
        source.set(() -> {
            throw new IOException("keycloak down");
        });

        assertThatThrownBy(() -> cache.findByKeyId("kid-1")).isInstanceOf(JwtException.class);
    }

    private static String sign(OctetKeyPair signingKey, String subject) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.EdDSA).keyID(signingKey.getKeyID()).build(),
                new JWTClaimsSet.Builder().subject(subject).build());
        jwt.sign(new Ed25519Signer(signingKey));
        return jwt.serialize();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
