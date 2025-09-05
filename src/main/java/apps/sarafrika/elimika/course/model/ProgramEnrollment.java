package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.course.util.converter.EnrollmentStatusConverter;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
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
@Table(name = "program_enrollments")
public class ProgramEnrollment extends BaseEntity {

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "enrollment_date")
    private LocalDateTime enrollmentDate;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @Column(name = "status")
    @Convert(converter = EnrollmentStatusConverter.class)
    private EnrollmentStatus status;

    @Column(name = "progress_percentage")
    private BigDecimal progressPercentage;

    @Column(name = "final_grade")
    private BigDecimal finalGrade;
}