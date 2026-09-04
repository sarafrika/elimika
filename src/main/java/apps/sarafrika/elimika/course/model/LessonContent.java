package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
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
    @Filterable
    private UUID lessonUuid;

    @Column(name = "content_type_uuid")
    @Filterable
    private UUID contentTypeUuid;

    @Column(name = "title")
    @Filterable
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
    @Filterable
    private Integer displayOrder;

    @Column(name = "is_required")
    @Filterable
    private Boolean isRequired;

    /**
     * On draft content, the live content it will be promoted onto. NULL means the edit adds
     * it. Preserves live content uuids so content_progress stays valid.
     */
    @Column(name = "source_content_uuid")
    private UUID sourceContentUuid;
}