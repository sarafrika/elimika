package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * An approved version of a course's content.
 * <p>
 * {@link ContentModerationHistory} records moderation decisions but no content; this records
 * the content itself. A row is written each time an edit is promoted onto the live course,
 * giving a durable history of what the course looked like at each approved version.
 */
@Entity
@Table(name = "course_version_snapshots")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseVersionSnapshot extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "version_number")
    private Integer versionNumber;

    /**
     * Full course tree: course fields, category uuids, lessons and their content. Media
     * fields hold storage keys rather than resolved URLs.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot")
    private JsonNode snapshot;

    @Column(name = "pending_edit_uuid")
    private UUID pendingEditUuid;
}
