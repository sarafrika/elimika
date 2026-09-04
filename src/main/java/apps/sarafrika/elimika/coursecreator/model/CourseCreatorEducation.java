package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
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
    @Filterable
    private UUID courseCreatorUuid;

    @Column(name = "qualification")
    @Filterable
    private String qualification;

    @Column(name = "field_of_study")
    @Filterable
    private String fieldOfStudy;

    @Column(name = "school_name")
    @Filterable
    private String schoolName;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "year_completed")
    private Integer yearCompleted;

    @Column(name = "certificate_number")
    private String certificateNumber;
}
