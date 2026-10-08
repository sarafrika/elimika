package apps.sarafrika.elimika.perf.internal;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RumRateLimiterTest {

    private final RumRateLimiter limiter = new RumRateLimiter(3, 2,
            Clock.fixed(Instant.parse("2026-10-08T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void anonymousCallersAreLimitedPerIp() {
        MockHttpServletRequest first = request("10.0.0.1");
        MockHttpServletRequest second = request("10.0.0.2");

        assertThat(limiter.tryAcquire(null, first)).isTrue();
        assertThat(limiter.tryAcquire(null, first)).isTrue();
        assertThat(limiter.tryAcquire(null, first)).isFalse();
        assertThat(limiter.tryAcquire(null, second)).isTrue();
    }

    @Test
    void signedInCallersGetTheirOwnLargerBudget() {
        MockHttpServletRequest shared = request("10.0.0.9");

        assertThat(limiter.tryAcquire("user-a", shared)).isTrue();
        assertThat(limiter.tryAcquire("user-a", shared)).isTrue();
        assertThat(limiter.tryAcquire("user-a", shared)).isTrue();
        assertThat(limiter.tryAcquire("user-a", shared)).isFalse();
        assertThat(limiter.tryAcquire("user-b", shared)).isTrue();
    }

    private static MockHttpServletRequest request(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", ip);
        return request;
    }
}
