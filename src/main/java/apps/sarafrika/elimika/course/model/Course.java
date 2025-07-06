package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "courses")
public class Course extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "category_uuid")
    private UUID categoryUuid;

    @Column(name = "difficulty_uuid")
    private UUID difficultyUuid;

    @Column(name = "description")
    private String description;

    @Column(name = "objectives")
    private String objectives;

    @Column(name = "prerequisites")
    private String prerequisites;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "class_limit")
    private Integer classLimit;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "age_lower_limit")
    private Integer ageLowerLimit;

    @Column(name = "age_upper_limit")
    private Integer ageUpperLimit;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "intro_video_url")
    private String introVideoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContentStatus status;

    @Column(name = "active")
    private Boolean active;
}