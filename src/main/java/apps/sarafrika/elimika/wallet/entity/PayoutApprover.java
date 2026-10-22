package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An organisation admin named to manage the organisation's payout numbers and withdraw its money. */
@Entity
@Table(name = "payout_approvers")
@Getter
@Setter
@NoArgsConstructor
public class PayoutApprover extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "added_by_user_uuid")
    private UUID addedByUserUuid;
}
