package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "organisation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organisation extends BaseEntity {

    @Column(name = "name")
    @Filterable
    private String name;

    @Column(name = "description")
    @Filterable
    private String description;

    @Column(name = "active")
    @Filterable
    private boolean active = true;


    @Column(name = "licence_no")
    private String licenceNo;


    @Column(name = "slug")
    @Filterable
    private String slug;

    @Column(name = "lat")
    private BigDecimal latitude;

    @Column(name = "long")
    private BigDecimal longitude;

    @Column(name = "deleted")
    @Filterable
    private boolean deleted = false;


    @Column(name = "location")
    @Filterable
    private String location;

    @Column(name = "country")
    private String country;

    @Column(name = "admin_verified")
    @Filterable
    private Boolean adminVerified = false;

    /** When the organisation submitted itself for admin verification; null if never submitted. */
    @Column(name = "verification_requested_at")
    private LocalDateTime verificationRequestedAt;
}