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
 * An ordered schooling level ("Grade 7", "Form 2") that a {@link StudentGroup} is filed under.
 * <p>
 * Rows with a {@code null} organisationUuid are the shared platform catalogue every tenant reads;
 * the column exists so a school on a different curriculum can add its own levels later without a
 * schema change. {@code tierOrder} is gapped by tens so a new level can be inserted between two
 * existing ones without renumbering the sequence.
 */
@Entity
@Table(name = "academic_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademicTier extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "tier_order")
    private Integer tierOrder;

    @Column(name = "education_system")
    private String educationSystem;

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "description")
    private String description;
}
