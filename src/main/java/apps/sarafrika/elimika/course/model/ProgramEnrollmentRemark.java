package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** One instructor progress remark on a learner's programme; never edited. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "program_enrollment_remarks")
public class ProgramEnrollmentRemark extends BaseEntity {

    @Column(name = "program_enrollment_uuid")
    private UUID programEnrollmentUuid;

    @Column(name = "strengths")
    private String strengths;

    @Column(name = "improvements")
    private String improvements;

    @Column(name = "recommendations")
    private String recommendations;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "author_user_uuid")
    private UUID authorUserUuid;
}
