package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.CourseAssessmentAggregationStrategyConverter;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_assessments")
public class CourseAssessment extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "assessment_type")
    private String assessmentType;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "weight_percentage")
    private BigDecimal weightPercentage;

    @Column(name = "aggregation_strategy")
    @Convert(converter = CourseAssessmentAggregationStrategyConverter.class)
    private CourseAssessmentAggregationStrategy aggregationStrategy;

    @Column(name = "rubric_uuid")
    private UUID rubricUuid;

    @Column(name = "is_required")
    private Boolean isRequired;
}
