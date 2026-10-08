package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.AgeGroupOwnerTypeConverter;
import apps.sarafrika.elimika.course.util.enums.AgeGroupOwnerType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A named age band: saved by an instructor or organisation, or copied into a training application. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "age_groups")
public class AgeGroup extends BaseEntity {

    @Column(name = "owner_type")
    @Convert(converter = AgeGroupOwnerTypeConverter.class)
    private AgeGroupOwnerType ownerType;

    @Column(name = "owner_uuid")
    private UUID ownerUuid;

    @Column(name = "name")
    private String name;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "position")
    private Integer position;
}
