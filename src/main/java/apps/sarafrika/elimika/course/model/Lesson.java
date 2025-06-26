package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.enums.LessonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter @Setter
public class Lesson extends BaseEntity {

    @Column(name = "lesson_no")
    private Integer lessonNo;

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "lesson_name")
    private String lessonName;

    @Column(name = "lesson_description")
    private String lessonDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", columnDefinition = "lesson_content_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private LessonType lessonType;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;
}