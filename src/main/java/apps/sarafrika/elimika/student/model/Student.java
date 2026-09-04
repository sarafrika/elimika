package apps.sarafrika.elimika.student.model;

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
@Table(name = "students")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Student extends BaseEntity {
    @Column(name = "user_uuid")
    @Filterable
    private UUID userUuid;

    @Column(name = "full_name")
    @Filterable
    private String fullName;

    @Column(name = "guardian_1_name")
    private String firstGuardianName;

    @Column(name = "guardian_1_mobile")
    private String firstGuardianMobile;

    @Column(name = "guardian_2_name")
    private String secondGuardianName;

    @Column(name = "guardian_2_mobile")
    private String secondGuardianMobile;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "demographic_tag")
    private String demographicTag;
}
