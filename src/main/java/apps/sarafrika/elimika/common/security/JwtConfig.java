package apps.sarafrika.elimika.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

@Configuration @Slf4j
public class JwtConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkUrl;

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            try {
                log.info("Validating JWT token: {}", token);
                SignedJWT signedJWT = (SignedJWT) JWTParser.parse(token);
                JWSAlgorithm algorithm = signedJWT.getHeader().getAlgorithm();

                if (!validateToken(signedJWT, algorithm)) {
                    throw new JwtException("Invalid JWT signature for algorithm: " + algorithm);
                }

                return convertToJwt(signedJWT);

            } catch (ParseException | JOSEException e) {
                throw new JwtException("Failed to parse or verify the JWT token", e);
            }
        };
    }

    private boolean validateToken(SignedJWT signedJWT, JWSAlgorithm algorithm) throws JOSEException {
        return switch (algorithm.getName()) {
            case "RS256" -> validateRS256(signedJWT);
            case "HS256" -> validateHMAC(signedJWT, "your-secret-key");
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

    private boolean validateHMAC(SignedJWT signedJWT, String secretKey) throws JOSEException {
        return signedJWT.verify(new MACVerifier(secretKey));
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
            JWKSet jwkSet = JWKSet.load(new URL(jwkUrl));
            String keyId = signedJWT.getHeader().getKeyID();

            JWK jwk = jwkSet.getKeys().stream()
                    .filter(k -> k.getKeyID().equals(keyId))
                    .findFirst()
                    .orElseThrow(() -> new JwtException("No matching public key found for key ID: " + keyId));

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
