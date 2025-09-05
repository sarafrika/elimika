package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Junction entity for many-to-many relationship between courses and categories
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_category_mappings")
@Builder
public class CourseCategoryMapping extends BaseEntity {

    @Column(name = "course_uuid", nullable = false)
    private UUID courseUuid;

    @Column(name = "category_uuid", nullable = false)
    private UUID categoryUuid;

    // Optional: You can add ManyToOne relationships for easier navigation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_uuid", referencedColumnName = "uuid", insertable = false, updatable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_uuid", referencedColumnName = "uuid", insertable = false, updatable = false)
    private Category category;
}