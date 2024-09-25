package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.shared.audit.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Lesson extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int lessonOrder;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isPublished;

    private Long classId;

}

