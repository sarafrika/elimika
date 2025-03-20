package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prerequisite")
public class Prerequisite extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "required_for_course_uuid")
    private UUID requiredForCourseUuid;

    @Column(name = "minimum_score")
    private BigDecimal minimumScore;

    @Column(name = "prerequisite_type_uuid")
    private UUID prerequisiteTypeUuid;

}
