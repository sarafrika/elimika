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
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenValidationTest {

    private static final String ISSUER = "https://signin.example.test/realms/elimika";
    private static final Instant NOW = Instant.parse("2026-10-12T08:00:00Z");

    private OctetKeyPair key;
    private JwtDecoder decoder;
    private Clock clock;
    private CachedJwkSet keys;

    @BeforeEach
    void setUp() throws Exception {
        key = new OctetKeyPairGenerator(Curve.Ed25519).keyID("kid-1").generate();
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        keys = new CachedJwkSet(() -> new JWKSet(key.toPublicJWK()), Duration.ofMinutes(5),
                Duration.ofSeconds(30), clock);
        decoder = new JwtConfig(keys, JwtConfig.tokenValidator(ISSUER, clock)).jwtDecoder();
    }

    @Test
    void acceptsAValidToken() throws Exception {
        String token = sign(c -> c.issuer(ISSUER).issueTime(at(-60)).expirationTime(at(240)));

        assertThat(decoder.decode(token).getSubject()).isEqualTo("user-1");
    }

    @Test
    void rejectsATokenExpiredLongAgo() throws Exception {
        String token = sign(c -> c.issuer(ISSUER).issueTime(at(-22 * 86_400 - 300))
                .expirationTime(at(-22 * 86_400)));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsATokenExpiredBeyondTheClockSkew() throws Exception {
        String token = sign(c -> c.issuer(ISSUER).expirationTime(at(-61)));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void toleratesExpiryWithinTheClockSkew() throws Exception {
        String token = sign(c -> c.issuer(ISSUER).expirationTime(at(-30)));

        assertThat(decoder.decode(token).getSubject()).isEqualTo("user-1");
    }

    @Test
    void rejectsATokenNotYetValid() throws Exception {
        String token = sign(c -> c.issuer(ISSUER).notBeforeTime(at(120)).expirationTime(at(420)));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsATokenFromAnotherIssuer() throws Exception {
        String token = sign(c -> c.issuer("https://evil.example.test/realms/elimika").expirationTime(at(240)));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsATokenWithoutExpiry() throws Exception {
        String token = sign(c -> c.issuer(ISSUER));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void withoutAConfiguredIssuerStillRejectsExpiredTokens() throws Exception {
        JwtDecoder lenient = new JwtConfig(keys, JwtConfig.tokenValidator("", clock)).jwtDecoder();

        assertThat(lenient.decode(sign(c -> c.issuer("https://other.test").expirationTime(at(240))))).isNotNull();
        assertThatThrownBy(() -> lenient.decode(sign(c -> c.issuer(ISSUER).expirationTime(at(-61)))))
                .isInstanceOf(JwtValidationException.class);
    }

    private static Date at(long secondsFromNow) {
        return Date.from(NOW.plusSeconds(secondsFromNow));
    }

    private String sign(Consumer<JWTClaimsSet.Builder> claims) throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject("user-1");
        claims.accept(builder);
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.EdDSA).keyID(key.getKeyID()).build(),
                builder.build());
        jwt.sign(new Ed25519Signer(key));
        return jwt.serialize();
    }
}
