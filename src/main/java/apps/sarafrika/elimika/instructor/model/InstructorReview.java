package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "instructor_reviews")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class InstructorReview extends BaseEntity {

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "enrollment_uuid")
    private UUID enrollmentUuid;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "headline")
    private String headline;

    @Column(name = "comments")
    private String comments;

    @Column(name = "clarity_rating")
    private Integer clarityRating;

    @Column(name = "engagement_rating")
    private Integer engagementRating;

    @Column(name = "punctuality_rating")
    private Integer punctualityRating;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous;
}
