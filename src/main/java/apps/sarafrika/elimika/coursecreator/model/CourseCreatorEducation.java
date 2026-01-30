package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "course_creator_education")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseCreatorEducation extends BaseEntity {

    @Column(name = "course_creator_uuid")
    private UUID courseCreatorUuid;

    @Column(name = "qualification")
    private String qualification;

    @Column(name = "field_of_study")
    private String fieldOfStudy;

    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "year_completed")
    private Integer yearCompleted;

    @Column(name = "certificate_number")
    private String certificateNumber;
}
