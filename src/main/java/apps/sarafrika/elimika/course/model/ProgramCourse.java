package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
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
    @Filterable
    private UUID programUuid;

    @Column(name = "course_uuid")
    @Filterable
    private UUID courseUuid;

    @Column(name = "sequence_order")
    @Filterable
    private Integer sequenceOrder;

    @Column(name = "is_required")
    @Filterable
    private Boolean isRequired;

    @Column(name = "prerequisite_course_uuid")
    private UUID prerequisiteCourseUuid;
}