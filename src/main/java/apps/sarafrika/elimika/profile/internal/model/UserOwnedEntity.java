package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A profile row owned by one user. */
@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class UserOwnedEntity extends BaseEntity {

    @Column(name = "user_uuid")
    @Filterable
    private UUID userUuid;
}
