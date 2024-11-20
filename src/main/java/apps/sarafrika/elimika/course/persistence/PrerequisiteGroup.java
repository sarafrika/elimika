package apps.sarafrika.elimika.course.persistence;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrerequisiteGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupType groupType;

    public enum GroupType {
        AND,
        OR
    }
}
