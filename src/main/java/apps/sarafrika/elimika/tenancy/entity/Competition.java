package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An organisation-scoped competition/event.
 */
@Entity
@Table(name = "competitions")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Competition extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "name")
    private String name;

    @Column(name = "category")
    private String category;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "venue_name")
    private String venueName;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "status")
    private String status;

    @Column(name = "description")
    private String description;
}
