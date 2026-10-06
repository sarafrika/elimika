package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_memberships")
@Getter
@Setter
@NoArgsConstructor
public class UserMembership extends UserOwnedEntity {

    @Column(name = "organization_name")
    @Filterable
    private String organizationName;

    @Column(name = "membership_number")
    @Filterable
    private String membershipNumber;

    @Column(name = "start_date")
    @Filterable
    private LocalDate startDate;

    @Column(name = "end_date")
    @Filterable
    private LocalDate endDate;

    @Column(name = "is_active")
    @Filterable
    private Boolean isActive = Boolean.TRUE;
}
