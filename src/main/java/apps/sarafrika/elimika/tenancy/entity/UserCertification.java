package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_certifications")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCertification extends BaseEntity {
    @Column(name = "issued_date")
    private LocalDate issuedDate;
    @Column(name = "issued_by")
    private String issuedBy;
    @Column(name = "certificate_url")
    private String certificateUrl;
    @Column(name = "user_uuid")
    private UUID userUuid;
}
