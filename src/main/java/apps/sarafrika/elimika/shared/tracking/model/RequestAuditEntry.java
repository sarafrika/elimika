package apps.sarafrika.elimika.shared.tracking.model;

import java.time.LocalDateTime;

/**
 * One finished HTTP request, captured on the request thread and handed to the background audit writer.
 *
 * <p>Immutable and free of servlet or security objects, so it is safe to hold after the request has completed.
 * Values are already redacted and truncated to the {@code request_audit_log} column limits. {@code userUuid} is
 * not carried: the writer resolves it from {@code keycloakId} off the request path.
 *
 * @param userDomains comma-separated values of the JWT {@code user_domain} claim (the Keycloak user attribute),
 *                    or {@code null} for anonymous callers or tokens without the claim
 * @param createdDate request completion time in UTC
 * @param createdBy   the auditor name, the authentication name or {@code SYSTEM}, as the JPA auditor would set it
 */
public record RequestAuditEntry(
        String requestId,
        String httpMethod,
        String requestUri,
        String queryString,
        String ipAddress,
        String remoteHost,
        String userAgent,
        String referer,
        String sessionId,
        String headerSnapshot,
        Integer responseStatus,
        Long processingTimeMs,
        String authenticationName,
        String userEmail,
        String userFullName,
        String userDomains,
        String keycloakId,
        LocalDateTime createdDate,
        String createdBy
) {
}
