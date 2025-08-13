package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.Organisation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<Organisation, Long>, JpaSpecificationExecutor<Organisation> {
    Optional<Organisation> findByUuid(UUID uuid);

    Optional<Organisation> findByCode(String code);

    Optional<Organisation> findByName(String name);

    boolean existsBySlug(String slug);

    Page<Organisation> findByDeletedFalse(Pageable pageable);

    Optional<Organisation> findByUuidAndDeletedFalse(UUID uuid);
}
