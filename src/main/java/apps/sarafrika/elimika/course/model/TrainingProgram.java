package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.converter.ContentStatusConverter;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "training_programs")
public class TrainingProgram extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "category_uuid")
    private UUID categoryUuid;

    @Column(name = "description")
    private String description;

    @Column(name = "objectives")
    private String objectives;

    @Column(name = "prerequisites")
    private String prerequisites;

    @Column(name = "total_duration_hours")
    private Integer totalDurationHours;

    @Column(name = "total_duration_minutes")
    private Integer totalDurationMinutes;

    @Column(name = "class_limit")
    private Integer classLimit;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "is_published")
    private Boolean isPublished;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "status")
    @Convert(converter = ContentStatusConverter.class)
    private ContentStatus status;
}