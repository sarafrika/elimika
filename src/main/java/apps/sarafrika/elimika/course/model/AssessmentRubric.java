package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "assessment_rubrics")
public class AssessmentRubric extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "rubric_type")
    private String rubricType;

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContentStatus status;

    @Column(name = "is_active")
    private Boolean isActive;
}
