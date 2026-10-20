package apps.sarafrika.elimika.timetabling.model;

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

import java.time.LocalDateTime;
import java.util.UUID;

/** A session report exactly as it was submitted (or frozen at lock time). Never updated. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "session_report_revisions")
public class SessionReportRevision extends BaseEntity {

    @Column(name = "session_report_uuid")
    private UUID sessionReportUuid;

    @Column(name = "revision_number")
    private Integer revisionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot")
    private JsonNode snapshot;

    @Column(name = "submitted_by_user_uuid")
    private UUID submittedByUserUuid;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
