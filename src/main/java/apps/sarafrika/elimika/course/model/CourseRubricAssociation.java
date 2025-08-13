package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing the association between courses and rubrics
 * <p>
 * This entity supports a many-to-many relationship between courses and rubrics,
 * allowing rubrics to be reused across multiple courses while maintaining
 * context-specific metadata for each association.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_rubric_associations")
public class CourseRubricAssociation extends BaseEntity {

    @Column(name = "course_uuid", nullable = false)
    private UUID courseUuid;

    @Column(name = "rubric_uuid", nullable = false)
    private UUID rubricUuid;

    @Column(name = "associated_by", nullable = false)
    private UUID associatedBy;

    @Column(name = "association_date", nullable = false)
    private LocalDateTime associationDate;

    @Column(name = "is_primary_rubric")
    private Boolean isPrimaryRubric;

    @Column(name = "usage_context", length = 100)
    private String usageContext;
}