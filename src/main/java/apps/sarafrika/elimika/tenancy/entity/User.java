package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.enums.Gender;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor
public class User extends BaseEntity {
    @Column(name = "first_name")
    @Filterable
    private String firstName;

    @Column(name = "middle_name")
    @Filterable
    private String middleName;

    @Column(name = "last_name")
    @Filterable
    private String lastName;

    @Column(name = "email")
    @Filterable
    private String email;

    @Column(name = "username")
    @Filterable
    private String username;

    @Column(name = "user_no")
    @Filterable
    private String userNo;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "active", nullable = false)
    @Filterable
    private boolean active = true;

    @Column(name = "keycloak_id")
    private String keycloakId;

    @Column(name="gender", columnDefinition = "gender")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Filterable
    private Gender gender;

    /**
     * User's domain mappings (standalone domains not tied to organizations).
     * This is a read-only relationship used for querying.
     * Fetched lazily to avoid performance issues.
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "uuid",
            insertable = false, updatable = false)
    private java.util.List<UserDomainMapping> domainMappings;

    /**
     * User's organization domain mappings (organization-specific roles).
     * This is a read-only relationship used for querying.
     * Fetched lazily to avoid performance issues.
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "uuid",
            insertable = false, updatable = false)
    private java.util.List<UserOrganisationDomainMapping> organisationDomainMappings;
}
