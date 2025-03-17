package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class CourseLearningObjective extends BaseEntity {

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "objective")
    private String objective;

}
