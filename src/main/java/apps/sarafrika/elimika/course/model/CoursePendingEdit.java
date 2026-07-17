package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.PendingEditStatusConverter;
import apps.sarafrika.elimika.course.util.enums.PendingEditStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Review state for a course edit held on a shadow draft course.
 * <p>
 * The proposed content lives on the draft course referenced by {@code draftCourseUuid};
 * this row only tracks who submitted it, who reviewed it and how it was resolved. Resolved
 * rows are retained, so the table doubles as the edit-submission history for a course.
 */
@Entity
@Table(name = "course_pending_edits")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CoursePendingEdit extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "draft_course_uuid")
    private UUID draftCourseUuid;

    @Column(name = "status")
    @Convert(converter = PendingEditStatusConverter.class)
    private PendingEditStatus status;

    @Column(name = "submitted_by_uuid")
    private UUID submittedByUuid;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_by_uuid")
    private UUID reviewedByUuid;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_reason")
    private String reviewReason;
}
