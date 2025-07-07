package apps.sarafrika.elimika.tenancy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "user_domain")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class UserDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "uuid", unique = true)
    private UUID uuid;

    @Column(name = "domain_name", nullable = false, unique = true)
    private String domainName;
}
