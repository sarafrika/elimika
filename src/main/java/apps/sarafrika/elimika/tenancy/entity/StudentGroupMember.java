package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Membership linking a student (by user UUID) to a {@link StudentGroup}.
 */
@Entity
@Table(name = "student_group_members")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StudentGroupMember extends BaseEntity {

    @Column(name = "group_uuid")
    private UUID groupUuid;

    @Column(name = "student_uuid")
    private UUID studentUuid;
}
