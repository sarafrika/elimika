package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.course.util.converter.EnrollmentStatusConverter;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_enrollments")
public class CourseEnrollment extends BaseEntity {

    @Column(name = "student_uuid")
    @Filterable
    private UUID studentUuid;

    @Column(name = "course_uuid")
    @Filterable
    private UUID courseUuid;

    /**
     * The course version this enrolment was sold against, from
     * {@code course_version_snapshots.version_number}.
     * <p>
     * NULL follows the live course, and is correct in two cases: the enrolment predates version
     * pinning, or the course has never had an edit promoted and so has no version to point at.
     */
    @Column(name = "course_version")
    @Filterable
    private Integer courseVersion;

    @Column(name = "enrollment_date")
    @Filterable
    private LocalDateTime enrollmentDate;

    @Column(name = "completion_date")
    @Filterable
    private LocalDateTime completionDate;

    @Column(name = "status")
    @Convert(converter = EnrollmentStatusConverter.class)
    @Filterable
    private EnrollmentStatus status;

    @Column(name = "progress_percentage")
    private BigDecimal progressPercentage;

    @Column(name = "final_grade")
    private BigDecimal finalGrade;
}