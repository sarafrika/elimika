package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Lesson extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "lesson_order")
    private int lessonOrder;

    @Column(name = "is_published")
    private boolean isPublished;

    @Column(name = "course_id")
    private Long courseId;
}

