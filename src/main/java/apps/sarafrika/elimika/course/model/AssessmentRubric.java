package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.converter.ContentStatusConverter;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "assessment_rubrics")
public class AssessmentRubric extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;


    @Column(name = "rubric_type")
    private String rubricType;

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "status")
    @Convert(converter = ContentStatusConverter.class)
    private ContentStatus status;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "total_weight", precision = 5, scale = 2)
    private java.math.BigDecimal totalWeight;

    @Column(name = "weight_unit", length = 20)
    private String weightUnit;


    @Column(name = "uses_custom_levels")
    private Boolean usesCustomLevels;

    @Column(name = "max_score", precision = 5, scale = 2)
    private java.math.BigDecimal maxScore;

    @Column(name = "min_passing_score", precision = 5, scale = 2)
    private java.math.BigDecimal minPassingScore;
}
