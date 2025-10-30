package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
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

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lessons")
public class Lesson extends BaseEntity {

    // Lesson duration is defined when classes are scheduled; no time estimates are persisted here.
    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "lesson_number")
    private Integer lessonNumber;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "learning_objectives")
    private String learningObjectives;

    @Column(name = "status")
    @Convert(converter = ContentStatusConverter.class)
    private ContentStatus status;

    @Column(name = "active")
    private Boolean active;
}
