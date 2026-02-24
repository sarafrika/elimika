package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_versions")
public class CourseVersion extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "version_number")
    private Integer versionNumber;

    @Column(name = "snapshot_hash")
    private String snapshotHash;

    @Column(name = "snapshot_payload_json")
    private String snapshotPayloadJson;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
