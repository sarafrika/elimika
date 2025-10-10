package apps.sarafrika.elimika.shared.tracking.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "request_audit_log")
public class RequestAuditLog extends BaseEntity {

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "request_uri", nullable = false, columnDefinition = "text")
    private String requestUri;

    @Column(name = "query_string", columnDefinition = "text")
    private String queryString;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "remote_host", length = 255)
    private String remoteHost;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "referer", columnDefinition = "text")
    private String referer;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "header_snapshot", columnDefinition = "text")
    private String headerSnapshot;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "authentication_name", length = 255)
    private String authenticationName;

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "user_full_name", length = 255)
    private String userFullName;

    @Column(name = "user_domains", columnDefinition = "text")
    private String userDomains;

    @Column(name = "keycloak_id", length = 255)
    private String keycloakId;
}
