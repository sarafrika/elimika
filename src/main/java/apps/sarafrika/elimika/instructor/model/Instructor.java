package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor @Table(name = "instructors")
public class Instructor extends BaseEntity {

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "lat")
    private BigDecimal latitude;

    @Column(name = "long")
    private BigDecimal longitude;

    @Column(name = "website")
    private String website;

    @Column(name = "bio")
    private String bio;

    @Column(name = "professional_headline")
    private String professionalHeadline;

    @ElementCollection
    @CollectionTable(
            name = "course_instructor",
            joinColumns = @JoinColumn(name = "instructor_id")
    )
    @Column(name = "course_id")
    private Set<Long> courseIds;

}

