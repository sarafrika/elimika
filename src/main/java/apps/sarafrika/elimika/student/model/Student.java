package apps.sarafrika.elimika.student.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
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
    private UUID userUuid;
    @Column(name = "guardian_1_name")
    private String firstGuardianName;
    @Column(name = "guardian_1_mobile")
    private String firstGuardianMobile;
    @Column(name = "guardian_2_name")
    private String secondGuardianName;
    @Column(name = "guardian_2_mobile")
    private String secondGuardianMobile;
}
