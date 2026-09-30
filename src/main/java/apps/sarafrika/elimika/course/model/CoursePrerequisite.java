package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One prior course a course asks its learners to have taken: required when {@code isMandatory}, only
 * recommended otherwise.
 * <p>
 * The pair is the identity (unique in the table), so a draft's rows are reconciled onto the live
 * course by prerequisite uuid rather than through a {@code source_*_uuid} link.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "course_prerequisites")
public class CoursePrerequisite extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "prerequisite_course_uuid")
    private UUID prerequisiteCourseUuid;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    public CoursePrerequisite(UUID courseUuid, UUID prerequisiteCourseUuid, boolean isMandatory) {
        this.courseUuid = courseUuid;
        this.prerequisiteCourseUuid = prerequisiteCourseUuid;
        this.isMandatory = isMandatory;
    }
}
