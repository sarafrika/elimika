package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.tenancy.util.enums.OrganisationPayoutChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Where an organisation wants to be paid: an M-Pesa Paybill or Till, entered during onboarding. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "organisation_payout_details")
public class OrganisationPayoutDetails extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "channel")
    private OrganisationPayoutChannel channel;

    @Column(name = "business_number")
    private String businessNumber;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "account_name")
    private String accountName;
}
