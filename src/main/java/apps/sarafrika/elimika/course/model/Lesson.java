package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor @Table(name = "lesson")
public class Lesson extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "lesson_order")
    private int lessonOrder;

    @Column(name = "is_published")
    private boolean isPublished;

    @Column(name = "course_uuid")
    private UUID courseUuid;
}

