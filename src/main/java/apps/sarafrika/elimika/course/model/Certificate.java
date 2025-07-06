package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "certificates")
public class Certificate extends BaseEntity {

    @Column(name = "certificate_number")
    private String certificateNumber;

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "template_uuid")
    private UUID templateUuid;

    @Column(name = "issued_date")
    private LocalDateTime issuedDate;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @Column(name = "final_grade")
    private BigDecimal finalGrade;

    @Column(name = "certificate_url")
    private String certificateUrl;

    @Column(name = "is_valid")
    private Boolean isValid;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason")
    private String revokedReason;
}