package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
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
    private UUID organisationUuid;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "address")
    private String address;


    @Column(name = "poc_name")
    private String pocName;

    @Column(name = "poc_email")
    private String pocEmail;

    @Column(name = "poc_telephone")
    private String pocTelephone;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "deleted")
    private boolean deleted = false;
}