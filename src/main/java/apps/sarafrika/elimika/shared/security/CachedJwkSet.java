package apps.sarafrika.elimika.shared.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Keycloak's signing keys, held in memory instead of fetched from the JWKS endpoint on every request.
 * Expired sets are reloaded by one thread while others keep using the old one; an unknown key id
 * forces a reload (key rotation) at most once per {@code minRefreshInterval}.
 */
@Slf4j
final class CachedJwkSet {

    private record Snapshot(JWKSet keys, Instant loadedAt) {
    }

    private final Callable<JWKSet> loader;
    private final Duration ttl;
    private final Duration minRefreshInterval;
    private final Clock clock;
    private final ReentrantLock reloadLock = new ReentrantLock();

    private volatile Snapshot snapshot;
    private volatile Instant lastAttempt = Instant.MIN;

    CachedJwkSet(Callable<JWKSet> loader, Duration ttl, Duration minRefreshInterval, Clock clock) {
        this.loader = loader;
        this.ttl = ttl;
        this.minRefreshInterval = minRefreshInterval;
        this.clock = clock;
    }

    JWK findByKeyId(String keyId) {
        Snapshot current = snapshot;
        if (current == null) {
            current = reload(null, true);
        } else if (isExpired(current) && refreshAllowed()) {
            current = reload(current, false);
        }

        JWK jwk = current.keys().getKeyByKeyId(keyId);
        if (jwk == null && refreshAllowed()) {
            current = reload(current, true);
            jwk = current.keys().getKeyByKeyId(keyId);
        }
        if (jwk == null) {
            throw new JwtException("No matching public key found for key ID: " + keyId);
        }
        return jwk;
    }

    /** Loads the key set eagerly so the first request after startup does not pay for the fetch. */
    void warmUp() {
        reload(snapshot, true);
    }

    private boolean isExpired(Snapshot current) {
        return clock.instant().isAfter(current.loadedAt().plus(ttl));
    }

    private boolean refreshAllowed() {
        return clock.instant().isAfter(lastAttempt.plus(minRefreshInterval));
    }

    /** Reloads unless another thread already replaced {@code seen}; a failed refresh keeps the old keys. */
    private Snapshot reload(Snapshot seen, boolean waitForLock) {
        if (waitForLock) {
            reloadLock.lock();
        } else if (!reloadLock.tryLock()) {
            return seen;
        }
        try {
            Snapshot latest = snapshot;
            if (latest != seen) {
                return latest;
            }
            Instant now = clock.instant();
            lastAttempt = now;
            try {
                latest = new Snapshot(loader.call(), now);
                snapshot = latest;
            } catch (Exception e) {
                if (seen == null) {
                    throw new JwtException("Unable to load the JWK set", e);
                }
                log.warn("JWK set refresh failed; keeping the keys loaded at {}", seen.loadedAt(), e);
            }
            return latest;
        } finally {
            reloadLock.unlock();
        }
    }
}
