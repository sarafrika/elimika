package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.coursecreator.util.converter.CourseCreatorVerificationStatusConverter;
import apps.sarafrika.elimika.coursecreator.util.enums.CourseCreatorVerificationStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
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
    @Filterable
    private UUID userUuid;

    @Column(name = "full_name")
    @Filterable
    private String fullName;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "lat")
    private java.math.BigDecimal latitude;

    @Column(name = "long")
    private java.math.BigDecimal longitude;

    @Column(name = "bio")
    private String bio;

    @Column(name = "professional_headline")
    private String professionalHeadline;

    @Column(name = "website")
    private String website;

    @Column(name = "admin_verified")
    @Filterable
    private Boolean adminVerified;

    @Column(name = "verification_status")
    @Convert(converter = CourseCreatorVerificationStatusConverter.class)
    @Filterable
    private CourseCreatorVerificationStatus verificationStatus;

    @Column(name = "verification_requested_at")
    private LocalDateTime verificationRequestedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_reason")
    private String reviewReason;
}
