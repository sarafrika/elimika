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
@Table(name = "course_bundle_courses")
public class CourseBundleCourse extends BaseEntity {

    @Column(name = "bundle_uuid")
    private UUID bundleUuid;

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "sequence_order")
    private Integer sequenceOrder;

    @Column(name = "is_required")
    private Boolean isRequired;
}