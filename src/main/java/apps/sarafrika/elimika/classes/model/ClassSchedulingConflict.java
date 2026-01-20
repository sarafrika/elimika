package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
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

@Entity
@Table(name = "class_scheduling_conflicts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClassSchedulingConflict extends BaseEntity {

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "requested_start")
    private LocalDateTime requestedStart;

    @Column(name = "requested_end")
    private LocalDateTime requestedEnd;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "reasons")
    private String[] reasons;

    @Column(name = "is_resolved")
    private Boolean isResolved;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
