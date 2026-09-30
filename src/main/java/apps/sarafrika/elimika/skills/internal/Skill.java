package apps.sarafrika.elimika.skills.internal;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/** One entry of the admin-curated skills taxonomy. */
@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
public class Skill extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "slug")
    private String slug;

    @Column(name = "parent_uuid")
    private UUID parentUuid;

    @Column(name = "aliases")
    private String[] aliases;

    @Column(name = "active")
    private Boolean active;

    public List<String> aliasList() {
        return aliases == null ? List.of() : List.of(aliases);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public SkillSummary toSummary() {
        return new SkillSummary(getUuid(), name, slug, parentUuid, aliasList(), isActive());
    }
}
