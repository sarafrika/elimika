package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    List<StudentGroup> findByOrganisationUuidOrderByNameAsc(UUID organisationUuid);

    /*
     * The three filtered variants are spelled out rather than folded into one query with
     * ":branchUuid IS NULL OR ..." because the group list is the Groups page's first call and a
     * derived query keeps each filter combination a plain index lookup.
     */

    List<StudentGroup> findByOrganisationUuidAndBranchUuidOrderByNameAsc(UUID organisationUuid, UUID branchUuid);

    List<StudentGroup> findByOrganisationUuidAndTierUuidOrderByNameAsc(UUID organisationUuid, UUID tierUuid);

    List<StudentGroup> findByOrganisationUuidAndBranchUuidAndTierUuidOrderByNameAsc(
            UUID organisationUuid, UUID branchUuid, UUID tierUuid);

    Optional<StudentGroup> findByUuid(UUID uuid);

    List<StudentGroup> findByUuidIn(Collection<UUID> uuids);

    boolean existsByUuid(UUID uuid);
}
