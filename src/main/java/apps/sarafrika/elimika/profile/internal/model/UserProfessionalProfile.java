package apps.sarafrika.elimika.profile.internal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "user_professional_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfessionalProfile extends UserOwnedEntity {

    @Column(name = "bio")
    private String bio;

    @Column(name = "professional_headline")
    private String professionalHeadline;

    @Column(name = "website")
    private String website;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "lat")
    private BigDecimal latitude;

    @Column(name = "long")
    private BigDecimal longitude;
}
