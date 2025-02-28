package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long>, JpaSpecificationExecutor<UserGroup> {
    Optional<UserGroup> findByUuid(UUID uuid);

    Page<UserGroup> findByOrganisationUuid(UUID organisationUuid, Pageable pageable);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    List<UserGroup> findByUsers_Id(Long userId);

}
