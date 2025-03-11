package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organisation")
@Getter
@Setter @NoArgsConstructor @AllArgsConstructor
public class Organisation extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "code")
    private String code;

    @Column(name = "domain")
    private String domain;

    @Column(name = "slug")
    private String slug;

    @Column(name = "auth_realm")
    private String authRealm;

    @Column(name = "keycloak_id")
    private String keycloakId;

    @OneToMany(mappedBy = "organisation")
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "organisation")
    private List<UserGroup> userGroups = new ArrayList<>();
}
