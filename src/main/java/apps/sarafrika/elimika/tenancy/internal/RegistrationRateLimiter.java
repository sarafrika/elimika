package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.tenancy.config.RegistrationProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

/** Caps registration and resend attempts per client IP and per email, in memory like {@link UserLookupRateLimiter}. */
@Component
public class RegistrationRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final UserLookupRateLimiter perIp;
    private final UserLookupRateLimiter perEmail;

    public RegistrationRateLimiter(RegistrationProperties properties) {
        this.perIp = new UserLookupRateLimiter(properties.getMaxAttemptsPerIpPerHour(), WINDOW,
                MAX_TRACKED_KEYS, Clock.systemUTC());
        this.perEmail = new UserLookupRateLimiter(properties.getMaxAttemptsPerEmailPerHour(), WINDOW,
                MAX_TRACKED_KEYS, Clock.systemUTC());
    }

    /** Counts one attempt; false once either the address or the email is over its hourly limit. */
    public boolean tryAcquire(HttpServletRequest request, String email) {
        boolean ipAllowed = perIp.tryAcquire("ip:" + clientIp(request));
        boolean emailAllowed = email == null || perEmail.tryAcquire("email:" + email.trim().toLowerCase(Locale.ROOT));
        return ipAllowed && emailAllowed;
    }

    /** Proxy-set address first; only the last X-Forwarded-For hop is trusted, as clients control the rest. */
    public static String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        String cloudflare = request.getHeader("CF-Connecting-IP");
        if (StringUtils.hasText(cloudflare)) {
            return cloudflare.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
