package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.CourseTrainingRequirementProviderConverter;
import apps.sarafrika.elimika.course.util.converter.CourseTrainingRequirementTypeConverter;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingRequirementProvider;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingRequirementType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
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
@Table(name = "course_training_requirements")
@NoArgsConstructor
@AllArgsConstructor
public class CourseTrainingRequirement extends BaseEntity {

    @Column(name = "course_uuid", nullable = false)
    private UUID courseUuid;

    @Column(name = "requirement_type", nullable = false)
    @Convert(converter = CourseTrainingRequirementTypeConverter.class)
    private CourseTrainingRequirementType requirementType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "provided_by")
    @Convert(converter = CourseTrainingRequirementProviderConverter.class)
    private CourseTrainingRequirementProvider providedBy;

    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = true;
}
