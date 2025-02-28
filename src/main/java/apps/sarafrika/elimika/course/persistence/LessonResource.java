package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResource extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "resource_url")
    private String resourceUrl;

    @Column(name = "display_order")
    private int displayOrder;

    @Column(name = "lesson_id")
    private Long lessonId;
}

