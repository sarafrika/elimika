package apps.sarafrika.elimika.timetabling.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.timetabling.util.converter.EnrollmentStatusConverter;
import apps.sarafrika.elimika.timetabling.util.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a student enrollment in a scheduled instance.
 * <p>
 * This entity tracks student enrollments in scheduled class instances and manages
 * attendance tracking. It links students to specific scheduled instances and
 * maintains enrollment status throughout the class lifecycle.
 * <p>
 * The entity follows the project's BaseEntity pattern for consistent UUID and audit fields.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment extends BaseEntity {
    
    @Column(name = "scheduled_instance_uuid")
    private UUID scheduledInstanceUuid;
    
    @Column(name = "student_uuid")
    private UUID studentUuid;
    
    @Column(name = "status")
    @Convert(converter = EnrollmentStatusConverter.class)
    private EnrollmentStatus status;
    
    @Column(name = "attendance_marked_at")
    private LocalDateTime attendanceMarkedAt;
}