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
 * An organisation-scoped, named collection of students (a cohort or stream).
 * <p>
 * A structured group names the branch that runs it and the academic tier it sits at; the
 * {@code groupType} is then just the stream label within that branch and tier. All three are
 * nullable because groups created before the structure existed carry none of them, and the
 * frontend renders those under an "Unassigned" pill rather than guessing.
 * <p>
 * Assembled through {@code StudentGroupFactory} rather than a builder.
 */
@Entity
@Table(name = "student_groups")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentGroup extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "group_type")
    private String groupType;

    @Column(name = "branch_uuid")
    private UUID branchUuid;

    @Column(name = "tier_uuid")
    private UUID tierUuid;

    /** Intended size. Advisory only — enrolment past it is reported, never blocked. */
    @Column(name = "capacity")
    private Integer capacity;
}
