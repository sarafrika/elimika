package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.internal.config.InternalCommerceProperties;
import apps.sarafrika.elimika.commerce.internal.service.RegionResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class RegionResolverImpl implements RegionResolver {

    private final InternalCommerceProperties internalCommerceProperties;
    private final Builder restClientBuilder;

    @Override
    public String resolveRegionCode(String requestedRegion, String clientIp) {
        String fromGeoIp = resolveFromIp(clientIp);
        if (StringUtils.hasText(fromGeoIp)) {
            return fromGeoIp;
        }

        String configured = normalize(internalCommerceProperties.getDefaultRegion());
        if (StringUtils.hasText(configured)) {
            return configured;
        }
        return normalize(requestedRegion);
    }

    private String resolveFromIp(String suppliedIp) {
        if (!Boolean.TRUE.equals(internalCommerceProperties.getGeoipEnabled())) {
            return null;
        }
        String ip = firstNonEmpty(suppliedIp, extractClientIp());
        if (!StringUtils.hasText(ip) || isLocalAddress(ip)) {
            return null;
        }
        try {
            RestClient restClient = geoRestClient();
            String endpoint = internalCommerceProperties.getGeoipCountryEndpoint();
            String response = restClient.get()
                    .uri(endpoint, ip)
                    .retrieve()
                    .body(String.class);
            String normalized = normalize(response);
            if (isCountryCode(normalized)) {
                return normalized;
            }
        } catch (Exception ignored) {
            // Fail silently and fall back
        }
        return null;
    }

    private RestClient geoRestClient() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        return restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    private String extractClientIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return null;
        }
        HttpServletRequest request = servletAttrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isLocalAddress(String ip) {
        return ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("172.16.")
                || ip.startsWith("127.")
                || ip.equalsIgnoreCase("::1")
                || ip.startsWith("fd")
                || ip.startsWith("fe80");
    }

    private boolean isCountryCode(String value) {
        return StringUtils.hasText(value) && value.length() >= 2 && value.length() <= 3 && value.chars().allMatch(Character::isLetter);
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }
}
