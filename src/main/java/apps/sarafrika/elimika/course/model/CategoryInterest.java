package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** One course category a user is interested in, as a holder of one domain. */
@Entity
@Table(name = "category_interests")
@Getter
@Setter
@NoArgsConstructor
public class CategoryInterest extends BaseEntity {

    @Column(name = "user_uuid")
    @Filterable
    private UUID userUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_domain")
    @Filterable
    private UserDomain userDomain;

    @Column(name = "category_uuid")
    @Filterable
    private UUID categoryUuid;
}
