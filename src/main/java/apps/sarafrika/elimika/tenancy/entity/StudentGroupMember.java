package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Membership linking a student to a {@link StudentGroup}.
 * <p>
 * Assembled through {@code StudentGroupFactory} rather than a builder.
 */
@Entity
@Table(name = "student_group_members")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentGroupMember extends BaseEntity {

    @Column(name = "group_uuid")
    private UUID groupUuid;

    /**
     * The student user's {@code users.uuid} — <strong>not</strong> a {@code students.uuid}, despite
     * the column name. Every writer supplies uuids taken from the organisation user lookups and the
     * roster query joins this straight to {@code users}.
     */
    @Column(name = "student_uuid")
    private UUID studentUuid;
}
