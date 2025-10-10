package apps.sarafrika.elimika.shared.tracking.service;

import apps.sarafrika.elimika.shared.tracking.entity.RequestAuditLog;
import apps.sarafrika.elimika.shared.tracking.model.RequestUserMetadata;
import apps.sarafrika.elimika.shared.tracking.repository.RequestAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestAuditService {

    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 1024;
    private static final int MAX_HEADER_SNAPSHOT_LENGTH = 4000;

    private final RequestAuditLogRepository requestAuditLogRepository;
    private final RequestUserMetadataResolver userMetadataResolver;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRequest(HttpServletRequest request, int responseStatus, long processingTimeMs, String requestId) {
        try {
            RequestUserMetadata metadata = userMetadataResolver.resolve();
            String ipAddress = resolveClientIpAddress(request);
            String headerSnapshot = buildHeaderSnapshot(request, processingTimeMs);

            RequestAuditLog logEntry = new RequestAuditLog();
            logEntry.setRequestId(truncate(requestId, MAX_REQUEST_ID_LENGTH));
            logEntry.setHttpMethod(request.getMethod());
            logEntry.setRequestUri(request.getRequestURI());
            logEntry.setQueryString(request.getQueryString());
            logEntry.setIpAddress(ipAddress);
            logEntry.setRemoteHost(request.getRemoteHost());
            logEntry.setUserAgent(truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
            logEntry.setReferer(request.getHeader("Referer"));
            logEntry.setSessionId(request.getRequestedSessionId());
            logEntry.setHeaderSnapshot(truncate(headerSnapshot, MAX_HEADER_SNAPSHOT_LENGTH));
            logEntry.setResponseStatus(responseStatus);
            logEntry.setProcessingTimeMs(processingTimeMs);
            logEntry.setAuthenticationName(metadata.getAuthenticationName());
            logEntry.setUserUuid(metadata.getUserUuid());
            logEntry.setUserEmail(metadata.getEmail());
            logEntry.setUserFullName(metadata.getFullName());
            logEntry.setUserDomains(formatDomains(metadata));
            logEntry.setKeycloakId(metadata.getKeycloakId());

            requestAuditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Failed to record request audit entry", ex);
        }
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
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }

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

    private String formatDomains(RequestUserMetadata metadata) {
        List<String> domains = metadata.getDomains();
        if (metadata.isAnonymous() || domains == null || domains.isEmpty()) {
            return null;
        }
        return String.join(",", domains);
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
