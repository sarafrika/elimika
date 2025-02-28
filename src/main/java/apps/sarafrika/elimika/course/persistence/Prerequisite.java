package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prerequisite extends BaseEntity {

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "required_for_course_id")
    private Long requiredForCourseId;

    @Column(name = "minimum_score")
    private double minimumScore;

    @Column(name = "prerequisite_type_id")
    private Long prerequisiteTypeId;

}
