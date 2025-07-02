package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "program_courses")
public class ProgramCourse extends BaseEntity {

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "sequence_order")
    private Integer sequenceOrder;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "prerequisite_course_uuid")
    private UUID prerequisiteCourseUuid;
}