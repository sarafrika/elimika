package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.coursecreator.util.enums.AchievementType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "course_creator_achievements")
@Getter
@Setter
@NoArgsConstructor
public class CourseCreatorAchievement extends BaseEntity {

    @Column(name = "course_creator_uuid")
    private UUID courseCreatorUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "achievement_type")
    private AchievementType achievementType;

    @Column(name = "awarded_by")
    private String awardedBy;

    @Column(name = "awarded_on")
    private LocalDate awardedOn;

    @Column(name = "description")
    private String description;
}
