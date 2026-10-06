package apps.sarafrika.elimika.profile.internal.repository;

import apps.sarafrika.elimika.profile.internal.model.UserOwnedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reads shared by every user-owned profile table. */
@NoRepositoryBean
public interface UserOwnedRepository<E extends UserOwnedEntity> extends JpaRepository<E, Long>, JpaSpecificationExecutor<E> {

    Optional<E> findByUuid(UUID uuid);

    List<E> findByUserUuidOrderByIdAsc(UUID userUuid);

    List<E> findByUserUuidInOrderByIdAsc(Collection<UUID> userUuids);

    long countByUserUuid(UUID userUuid);
}
