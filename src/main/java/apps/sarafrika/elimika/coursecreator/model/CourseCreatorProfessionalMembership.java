package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "course_creator_professional_memberships")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseCreatorProfessionalMembership extends BaseEntity {

    @Column(name = "course_creator_uuid")
    private UUID courseCreatorUuid;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "membership_number")
    private String membershipNumber;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
