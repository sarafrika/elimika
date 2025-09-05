package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lesson_content_types")
public class ContentType extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "mime_types")
    private String[] mimeTypes;

    @Column(name = "max_file_size_mb")
    private Integer maxFileSizeMb;
}
