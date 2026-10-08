package apps.sarafrika.elimika.perf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One real-user timing sample; append-only and purged after the retention window. */
@Entity
@Table(name = "perf_rum_event")
@Getter
@Setter
@NoArgsConstructor
public class PerfRumEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "route_template")
    private String routeTemplate;

    @Column(name = "domain")
    private String domain;

    @Column(name = "metric")
    private String metric;

    @Column(name = "value_ms")
    private Double valueMs;

    @Column(name = "section")
    private String section;

    @Column(name = "network_type")
    private String networkType;

    @Column(name = "device_class")
    private String deviceClass;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "authenticated")
    private boolean authenticated;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "received_at")
    private Instant receivedAt;
}
