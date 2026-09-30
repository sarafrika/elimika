package apps.sarafrika.elimika.shared.tracking.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One impression, click or dismissal of a recommended item. {@code createdAt} is UTC and drives the
 * 180-day retention.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "discovery_events")
public class DiscoveryEvent extends BaseEntity {

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "surface")
    private String surface;

    @Column(name = "recommendation_id")
    private UUID recommendationId;

    @Column(name = "item_type")
    private String itemType;

    @Column(name = "item_uuid")
    private UUID itemUuid;

    @Column(name = "position")
    private Integer position;

    @Convert(converter = DiscoveryEventTypeConverter.class)
    @Column(name = "event_type")
    private DiscoveryEventType eventType;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "reason_codes")
    private String[] reasonCodes;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static DiscoveryEvent of(UUID userUuid, String surface, UUID recommendationId, String itemType,
                                    UUID itemUuid, int position, DiscoveryEventType eventType,
                                    String[] reasonCodes, String modelVersion, LocalDateTime createdAt) {
        DiscoveryEvent event = new DiscoveryEvent();
        event.userUuid = userUuid;
        event.surface = surface;
        event.recommendationId = recommendationId;
        event.itemType = itemType;
        event.itemUuid = itemUuid;
        event.position = position;
        event.eventType = eventType;
        event.reasonCodes = reasonCodes == null ? new String[0] : reasonCodes;
        event.modelVersion = modelVersion;
        event.createdAt = createdAt;
        return event;
    }
}
