package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lesson_contents")
public class LessonContent extends BaseEntity {

    @Column(name = "lesson_uuid")
    private UUID lessonUuid;

    @Column(name = "content_type_uuid")
    private UUID contentTypeUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "content_text")
    private String contentText;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_required")
    private Boolean isRequired;
}