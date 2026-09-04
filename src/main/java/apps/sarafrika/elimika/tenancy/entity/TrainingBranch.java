package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "training_branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainingBranch extends BaseEntity {

    @Column(name = "organisation_uuid")
    @Filterable
    private UUID organisationUuid;

    @Column(name = "branch_name")
    @Filterable
    private String branchName;

    @Column(name = "address")
    @Filterable
    private String address;


    @Column(name = "lat")
    private java.math.BigDecimal latitude;

    @Column(name = "long")
    private java.math.BigDecimal longitude;

    @Column(name = "poc_name")
    @Filterable
    private String pocName;

    @Column(name = "poc_email")
    private String pocEmail;

    @Column(name = "poc_telephone")
    private String pocTelephone;

    @Column(name = "active")
    @Filterable
    private boolean active = true;

    @Column(name = "deleted")
    @Filterable
    private boolean deleted = false;
}