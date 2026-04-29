package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.ContentStatusConverter;
import apps.sarafrika.elimika.course.util.converter.PracticeActivityGroupingConverter;
import apps.sarafrika.elimika.course.util.converter.PracticeActivityTypeConverter;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.PracticeActivityGrouping;
import apps.sarafrika.elimika.course.util.enums.PracticeActivityType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lesson_practice_activities")
public class LessonPracticeActivity extends BaseEntity {

    @Column(name = "lesson_uuid")
    private UUID lessonUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "activity_type")
    @Convert(converter = PracticeActivityTypeConverter.class)
    private PracticeActivityType activityType;

    @Column(name = "grouping")
    @Convert(converter = PracticeActivityGroupingConverter.class)
    private PracticeActivityGrouping grouping;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "materials")
    private String[] materials;

    @Column(name = "expected_output")
    private String expectedOutput;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "status")
    @Convert(converter = ContentStatusConverter.class)
    private ContentStatus status;

    @Column(name = "active")
    private Boolean active;
}
