package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** The M-Pesa number an organisation wants to be paid on, entered during onboarding. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "organisation_payout_details")
public class OrganisationPayoutDetails extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    /** Stored as 2547XXXXXXXX or 2541XXXXXXXX. */
    @Column(name = "mpesa_number")
    private String mpesaNumber;
}
