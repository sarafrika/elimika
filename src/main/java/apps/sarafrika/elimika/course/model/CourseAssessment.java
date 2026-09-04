package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.CourseAssessmentAggregationStrategyConverter;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
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
    @Filterable
    private UUID courseUuid;

    @Column(name = "assessment_type")
    @Filterable
    private String assessmentType;

    @Column(name = "title")
    @Filterable
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "weight_percentage")
    private BigDecimal weightPercentage;

    @Column(name = "aggregation_strategy")
    @Convert(converter = CourseAssessmentAggregationStrategyConverter.class)
    private CourseAssessmentAggregationStrategy aggregationStrategy;

    @Column(name = "rubric_uuid")
    @Filterable
    private UUID rubricUuid;

    @Column(name = "sync_class_attendance")
    private Boolean syncClassAttendance;

    @Column(name = "is_required")
    @Filterable
    private Boolean isRequired;

    // NOT NULL in the schema with a DB default. Hibernate emits every mapped column on insert, so a
    // null here would be sent explicitly and defeat that default — initialise it on the Java side.
    @Column(name = "active", nullable = false)
    @Filterable
    private Boolean active = Boolean.TRUE;

    /**
     * On a draft assessment, the live assessment it will be promoted onto. NULL means the
     * edit adds it. Preserves live assessment uuids so assessment scores stay valid.
     */
    @Column(name = "source_assessment_uuid")
    private UUID sourceAssessmentUuid;
}
