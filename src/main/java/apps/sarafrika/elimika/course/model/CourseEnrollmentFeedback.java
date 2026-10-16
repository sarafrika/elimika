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

/** One instructor comment on a learner's enrolment. Never edited; the newest is the current comment. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_enrollment_feedback")
public class CourseEnrollmentFeedback extends BaseEntity {

    @Column(name = "enrollment_uuid")
    private UUID enrollmentUuid;

    @Column(name = "comments")
    private String comments;

    @Column(name = "author_user_uuid")
    private UUID authorUserUuid;
}
