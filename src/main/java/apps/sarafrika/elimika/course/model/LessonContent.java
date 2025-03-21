package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lesson_content")
public class LessonContent extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "display_order")
    private int displayOrder;

    @Column(name = "duration")
    private BigDecimal duration;

    @Column(name = "lesson_uuid")
    private UUID lessonUuid;

    @Column(name = "content_type_uuid")
    private UUID contentTypeUuid;
}
