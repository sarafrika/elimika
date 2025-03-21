package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lesson_resource")
public class LessonResource extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "resource_url")
    private String resourceUrl;

    @Column(name = "display_order")
    private int displayOrder;

    @Column(name = "lesson_uuid")
    private UUID lessonUuid;
}

