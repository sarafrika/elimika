package apps.sarafrika.elimika.shared.tracking.service;

import apps.sarafrika.elimika.shared.tracking.QueryStringRedactor;
import apps.sarafrika.elimika.shared.tracking.model.RequestAuditEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Captures a finished request as a {@link RequestAuditEntry} and hands it to the {@link RequestAuditWriter}.
 *
 * <p>Runs on the request thread, so it does no database work: the caller's identity is read from the JWT in the
 * security context ({@code sub}, {@code email}, {@code name} or {@code given_name}/{@code family_name}, and the
 * {@code user_domain} claim for {@code user_domains}). {@code user_uuid} is resolved later by the writer.
 *
 * <p>Column semantics versus the former synchronous lookup: {@code user_domains} now reflects the Keycloak
 * {@code user_domain} attribute carried in the token (null when absent) rather than the database domain mappings,
 * and a signed-in caller whose token lacks {@code email} or a name gets {@code null} there instead of the old
 * {@code unknown@example.com} / {@code Unknown User} placeholders.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequestAuditService {

    private static final String SYSTEM_USER = "SYSTEM";
    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final int MAX_METHOD_LENGTH = 10;
    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_SHORT_TEXT_LENGTH = 255;
    private static final int MAX_SESSION_ID_LENGTH = 128;
    private static final int MAX_CREATED_BY_LENGTH = 50;
    private static final int MAX_USER_AGENT_LENGTH = 1024;
    private static final int MAX_HEADER_SNAPSHOT_LENGTH = 4000;

    private final RequestAuditWriter requestAuditWriter;
    private final ObjectMapper objectMapper;

    public void recordRequest(HttpServletRequest request, int responseStatus, long processingTimeMs, String requestId) {
        try {
            requestAuditWriter.offer(buildEntry(request, responseStatus, processingTimeMs, requestId));
        } catch (Exception ex) {
            log.error("Failed to capture request audit entry", ex);
        }
    }

    RequestAuditEntry buildEntry(HttpServletRequest request, int responseStatus, long processingTimeMs,
                                 String requestId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticationName = extractAuthenticationName(authentication);
        Jwt jwt = authenticationName != null && authentication.getPrincipal() instanceof Jwt token ? token : null;

        String ipAddress = resolveClientIpAddress(request);
        return new RequestAuditEntry(
                truncate(requestId, MAX_REQUEST_ID_LENGTH),
                truncate(request.getMethod(), MAX_METHOD_LENGTH),
                Objects.requireNonNullElse(request.getRequestURI(), "/"),
                QueryStringRedactor.redact(request.getQueryString()),
                truncate(StringUtils.hasText(ipAddress) ? ipAddress : "unknown", MAX_IP_LENGTH),
                truncate(request.getRemoteHost(), MAX_SHORT_TEXT_LENGTH),
                truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH),
                request.getHeader("Referer"),
                truncate(request.getRequestedSessionId(), MAX_SESSION_ID_LENGTH),
                truncate(buildHeaderSnapshot(request, processingTimeMs), MAX_HEADER_SNAPSHOT_LENGTH),
                responseStatus,
                processingTimeMs,
                truncate(authenticationName, MAX_SHORT_TEXT_LENGTH),
                jwt == null ? null : truncate(jwt.getClaimAsString("email"), MAX_SHORT_TEXT_LENGTH),
                jwt == null ? null : truncate(resolveFullName(jwt), MAX_SHORT_TEXT_LENGTH),
                jwt == null ? null : resolveDomains(jwt),
                jwt == null ? null : truncate(jwt.getSubject(), MAX_SHORT_TEXT_LENGTH),
                LocalDateTime.now(ZoneOffset.UTC),
                truncate(authenticationName != null ? authenticationName : SYSTEM_USER, MAX_CREATED_BY_LENGTH)
        );
    }

    private String extractAuthenticationName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    private String resolveFullName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (StringUtils.hasText(name)) {
            return name;
        }
        String given = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");
        String joined = ((given == null ? "" : given) + " " + (family == null ? "" : family)).trim();
        return joined.isEmpty() ? null : joined;
    }

    private String resolveDomains(Jwt jwt) {
        Object claim = jwt.getClaim("user_domain");
        if (claim instanceof Collection<?> values) {
            String joined = values.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(","));
            return joined.isEmpty() ? null : joined;
        }
        if (claim != null && StringUtils.hasText(claim.toString())) {
            return claim.toString();
        }
        return null;
    }

    private String resolveClientIpAddress(HttpServletRequest request) {
        String[] headerCandidates = {
                "X-Forwarded-For",
                "X-Real-IP",
                "CF-Connecting-IP",
                "True-Client-IP"
        };

        for (String header : headerCandidates) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value)) {
                return extractClientIp(value);
            }
        }

        return request.getRemoteAddr();
    }

    private String extractClientIp(String headerValue) {
        String[] parts = headerValue.split(",");
        return parts.length > 0 ? parts[0].trim() : headerValue.trim();
    }

    private String buildHeaderSnapshot(HttpServletRequest request, long processingTimeMs) {
        Map<String, String> headers = new LinkedHashMap<>();
        addHeaderIfPresent(headers, "X-Request-Id", request.getHeader("X-Request-Id"));
        addHeaderIfPresent(headers, "X-Correlation-Id", request.getHeader("X-Correlation-Id"));
        addHeaderIfPresent(headers, "X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        addHeaderIfPresent(headers, "X-Real-IP", request.getHeader("X-Real-IP"));
        addHeaderIfPresent(headers, "CF-Connecting-IP", request.getHeader("CF-Connecting-IP"));
        addHeaderIfPresent(headers, "True-Client-IP", request.getHeader("True-Client-IP"));
        addHeaderIfPresent(headers, "Accept-Language", request.getHeader("Accept-Language"));
        addHeaderIfPresent(headers, "User-Agent", request.getHeader("User-Agent"));
        addHeaderIfPresent(headers, "Referer", request.getHeader("Referer"));
        headers.put("processingTimeMs", String.valueOf(processingTimeMs));

        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialise header snapshot", e);
            return null;
        }
    }

    private void addHeaderIfPresent(Map<String, String> headers, String key, String value) {
        if (StringUtils.hasText(value)) {
            headers.put(key, value);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
