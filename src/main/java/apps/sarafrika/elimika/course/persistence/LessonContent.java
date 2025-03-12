package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class LessonContent extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "display_order")
    private int displayOrder;

    @Column(name = "duration")
    private int duration;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "content_type_id")
    private Long contentTypeId;
}
