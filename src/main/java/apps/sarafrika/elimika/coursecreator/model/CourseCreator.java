package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
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
@AllArgsConstructor
@Table(name = "course_creators")
public class CourseCreator extends BaseEntity {

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "lat")
    private BigDecimal latitude;

    @Column(name = "long")
    private BigDecimal longitude;

    @Column(name = "bio")
    private String bio;

    @Column(name = "professional_headline")
    private String professionalHeadline;

    @Column(name = "website")
    private String website;

    @Column(name = "admin_verified")
    private Boolean adminVerified;
}
