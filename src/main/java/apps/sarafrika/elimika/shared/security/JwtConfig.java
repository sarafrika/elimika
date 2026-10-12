package apps.sarafrika.elimika.shared.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration @Slf4j
public class JwtConfig {

    private static final Duration JWK_SET_TTL = Duration.ofMinutes(5);
    private static final Duration JWK_SET_MIN_REFRESH_INTERVAL = Duration.ofSeconds(30);
    private static final int JWK_SET_CONNECT_TIMEOUT_MS = 2_000;
    private static final int JWK_SET_READ_TIMEOUT_MS = 3_000;
    private static final int JWK_SET_SIZE_LIMIT_BYTES = 512 * 1024;
    static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    private final CachedJwkSet jwkSet;
    private final OAuth2TokenValidator<Jwt> tokenValidator;

    @Autowired
    public JwtConfig(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkUrl,
                     @Value("${JWT_ISSUER_URI:}") String issuer) {
        this(new CachedJwkSet(() -> JWKSet.load(URI.create(jwkUrl).toURL(), JWK_SET_CONNECT_TIMEOUT_MS,
                JWK_SET_READ_TIMEOUT_MS, JWK_SET_SIZE_LIMIT_BYTES), JWK_SET_TTL, JWK_SET_MIN_REFRESH_INTERVAL,
                Clock.systemUTC()), tokenValidator(issuer, Clock.systemUTC()));
    }

    JwtConfig(CachedJwkSet jwkSet, OAuth2TokenValidator<Jwt> tokenValidator) {
        this.jwkSet = jwkSet;
        this.tokenValidator = tokenValidator;
    }

    /** Requires exp and rejects expired or not-yet-valid tokens; checks the issuer only when JWT_ISSUER_URI is set. */
    static OAuth2TokenValidator<Jwt> tokenValidator(String issuer, Clock clock) {
        JwtTimestampValidator timestamps = new JwtTimestampValidator(CLOCK_SKEW);
        timestamps.setClock(clock);
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>(List.of(
                new JwtClaimValidator<>(JwtClaimNames.EXP, exp -> exp != null), timestamps));
        if (issuer != null && !issuer.isBlank()) {
            validators.add(new JwtIssuerValidator(issuer));
        }
        return new DelegatingOAuth2TokenValidator<>(validators);
    }

    /** Fetches Keycloak's keys before traffic arrives; a failure here is retried on the first request. */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpJwkSet() {
        try {
            jwkSet.warmUp();
        } catch (RuntimeException e) {
            log.warn("Could not preload the JWK set; it will be fetched on the first request", e);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            try {
                SignedJWT signedJWT = (SignedJWT) JWTParser.parse(token);
                JWSAlgorithm algorithm = signedJWT.getHeader().getAlgorithm();

                if (!validateToken(signedJWT, algorithm)) {
                    throw new JwtException("Invalid JWT signature for algorithm: " + algorithm);
                }

                Jwt jwt = convertToJwt(signedJWT);
                OAuth2TokenValidatorResult result = tokenValidator.validate(jwt);
                if (result.hasErrors()) {
                    throw new JwtValidationException("Invalid JWT: " + result.getErrors().iterator().next()
                            .getDescription(), result.getErrors());
                }
                return jwt;

            } catch (ParseException | JOSEException e) {
                throw new JwtException("Failed to parse or verify the JWT token", e);
            }
        };
    }

    private boolean validateToken(SignedJWT signedJWT, JWSAlgorithm algorithm) throws JOSEException {
        return switch (algorithm.getName()) {
            case "RS256" -> validateRS256(signedJWT);
            case "EdDSA" -> validateEdDSA(signedJWT);
            case "ES256" -> validateES256(signedJWT);
            default -> throw new JwtException("Unsupported JWT algorithm: " + algorithm.getName());
        };
    }

    private Jwt convertToJwt(SignedJWT signedJWT) throws ParseException {
        return Jwt.withTokenValue(signedJWT.getParsedString())
                .headers(headers -> headers.putAll(signedJWT.getHeader().toJSONObject()))
                .claims(claims -> {
                    try {
                        signedJWT.getJWTClaimsSet().getClaims().forEach((key, value) -> {
                            if (value instanceof java.util.Date) {
                                claims.put(key, ((java.util.Date) value).toInstant());
                            } else {
                                claims.put(key, value);
                            }
                        });
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                })
                .build();
    }

    private boolean validateRS256(SignedJWT signedJWT) throws JOSEException {
        return validateWithJWK(signedJWT, jwk -> {
            RSAPublicKey publicKey = (RSAPublicKey) jwk.toRSAKey().toPublicKey();
            return new RSASSAVerifier(publicKey);
        });
    }

    private boolean validateEdDSA(SignedJWT signedJWT) throws JOSEException {
        return validateWithJWK(signedJWT, jwk -> new Ed25519Verifier(jwk.toOctetKeyPair()));
    }

    private boolean validateES256(SignedJWT signedJWT) throws JOSEException {
        return validateWithJWK(signedJWT, jwk -> {
            ECPublicKey publicKey = (ECPublicKey) jwk.toECKey().toPublicKey();
            return new ECDSAVerifier(publicKey);
        });
    }

    private boolean validateWithJWK(SignedJWT signedJWT, JWKValidator validator) throws JOSEException {
        try {
            JWK jwk = jwkSet.findByKeyId(signedJWT.getHeader().getKeyID());
            JWSVerifier verifier = validator.createVerifier(jwk);
            return signedJWT.verify(verifier);

        } catch (Exception e) {
            throw new JwtException("JWT verification failed", e);
        }
    }

    @FunctionalInterface
    private interface JWKValidator {
        JWSVerifier createVerifier(JWK jwk) throws JOSEException;
    }
}
