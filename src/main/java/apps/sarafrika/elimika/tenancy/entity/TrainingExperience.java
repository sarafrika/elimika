package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Table(name = "training_experience")
@Entity
@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
@Builder
public class TrainingExperience extends BaseEntity {
    @Column(name = "organisation_name")
    private String organisationName;
    @Column(name = "job_title")
    private String jobTitle;
    @Column(name = "work_description")
    private String workDescription;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "user_uuid")
    private UUID userUuid;
}
