package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "course_reviews")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseReview extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "headline")
    private String headline;

    @Column(name = "comments")
    private String comments;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous;
}
