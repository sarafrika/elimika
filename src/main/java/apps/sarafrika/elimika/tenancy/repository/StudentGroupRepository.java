package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    List<StudentGroup> findByOrganisationUuidOrderByNameAsc(UUID organisationUuid);

    Optional<StudentGroup> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);
}
