package apps.sarafrika.elimika.common.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "user_skills")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserSkill {
    @EmbeddedId
    private UserSkillId id;

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode  // This generates equals() and hashCode() for you
    public static class UserSkillId implements Serializable {
        private static final long serialVersionUID = 1L;

        private UUID userUuid;
        private UUID skillUuid;
    }
}