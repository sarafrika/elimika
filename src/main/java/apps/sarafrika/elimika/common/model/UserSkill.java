package apps.sarafrika.elimika.common.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity @Table(name = "user_skills")
@AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class UserSkill {
    @EmbeddedId
    private UserSkillId id;

    @Getter
    @Setter @Embeddable
    abstract static class UserSkillId implements Serializable {
        private UUID userUuid;
        private UUID skillUuid;
    }
}
