package apps.sarafrika.elimika.tenancy.internal;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class UserLookupRateLimiterTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-01T10:00:00Z"));
    private final Clock clock = new Clock() {
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
            return now.get();
        }
    };

    @Test
    void refusesOverTheLimitUntilTheWindowRollsOver() {
        UserLookupRateLimiter limiter = new UserLookupRateLimiter(3, Duration.ofMinutes(1), 100, clock);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();

        now.set(now.get().plusSeconds(60));
        assertThat(limiter.tryAcquire("a")).isTrue();
    }

    @Test
    void staysBoundedByEvictingTheLeastRecentCaller() {
        UserLookupRateLimiter limiter = new UserLookupRateLimiter(1, Duration.ofMinutes(1), 2, clock);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("b")).isTrue();
        assertThat(limiter.tryAcquire("c")).isTrue();
        // "a" was evicted, "c" is still tracked.
        assertThat(limiter.tryAcquire("c")).isFalse();
        assertThat(limiter.tryAcquire("a")).isTrue();
    }
}
