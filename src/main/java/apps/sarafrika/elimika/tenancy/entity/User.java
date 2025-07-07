package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.tenancy.enums.Gender;
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
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "username")
    private String username;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "keycloak_id")
    private String keycloakId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Column(name="gender", columnDefinition = "gender")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private Gender gender;
}