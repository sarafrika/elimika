package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "content_progress")
public class ContentProgress extends BaseEntity {

    @Column(name = "enrollment_uuid")
    private UUID enrollmentUuid;

    @Column(name = "content_uuid")
    private UUID contentUuid;

    @Column(name = "is_accessed")
    private Boolean isAccessed;

    @Column(name = "is_completed")
    private Boolean isCompleted;

    @Column(name = "access_count")
    private Integer accessCount;

    @Column(name = "first_accessed_at")
    private LocalDateTime firstAccessedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}