package apps.sarafrika.elimika.resourcing.model;

import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "organisation_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationResource extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "branch_uuid")
    private UUID branchUuid;

    @Column(name = "resource_type")
    private ResourceType resourceType;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "seat_capacity")
    private Integer seatCapacity;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "location_latitude")
    private BigDecimal locationLatitude;

    @Column(name = "location_longitude")
    private BigDecimal locationLongitude;

    @Column(name = "is_active")
    private Boolean isActive;
}
