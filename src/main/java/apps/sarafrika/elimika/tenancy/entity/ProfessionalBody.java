package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "professional_bodies")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ProfessionalBody extends BaseEntity {
    @Column(name = "body_name")
    private String bodyName;
    @Column(name = "membership_no")
    private String membershipNo;
    @Column(name = "member_since")
    private LocalDate memberSince;
    @Column(name = "user_uuid")
    private UUID userUuid;
}
