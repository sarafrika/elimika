package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.profile.spi.AchievementType;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_achievements")
@Getter
@Setter
@NoArgsConstructor
public class UserAchievement extends UserOwnedEntity {

    @Column(name = "title")
    @Filterable
    private String title;

    @Column(name = "achievement_type")
    @Filterable
    private AchievementType achievementType;

    @Column(name = "awarded_by")
    private String awardedBy;

    @Column(name = "awarded_on")
    @Filterable
    private LocalDate awardedOn;

    @Column(name = "description")
    private String description;
}
