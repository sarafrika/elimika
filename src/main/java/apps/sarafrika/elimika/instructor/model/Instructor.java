package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor @Table(name = "instructors")
public class Instructor extends BaseEntity {

    @Column(name = "user_uuid")
    @Filterable
    private UUID userUuid;

    @Column(name = "full_name")
    @Filterable
    private String fullName;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "lat")
    private BigDecimal latitude;

    @Column(name = "long")
    private BigDecimal longitude;

    @Column(name = "website")
    private String website;

    @Column(name = "bio")
    private String bio;

    @Column(name = "professional_headline")
    private String professionalHeadline;

    @Column(name="admin_verified")
    @Filterable
    private Boolean adminVerified;
}

