package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.AgeGroup;
import apps.sarafrika.elimika.course.util.enums.AgeGroupOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgeGroupRepository extends JpaRepository<AgeGroup, Long> {

    Optional<AgeGroup> findByUuid(UUID uuid);

    List<AgeGroup> findByOwnerTypeAndOwnerUuidInOrderByPositionAscIdAsc(AgeGroupOwnerType ownerType,
                                                                        Collection<UUID> ownerUuids);

    long countByOwnerTypeAndOwnerUuid(AgeGroupOwnerType ownerType, UUID ownerUuid);

    /** Lesson hours go with their group through the foreign key's ON DELETE CASCADE. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AgeGroup ageGroup WHERE ageGroup.ownerType = :ownerType AND ageGroup.ownerUuid = :ownerUuid")
    void deleteForOwner(@Param("ownerType") AgeGroupOwnerType ownerType, @Param("ownerUuid") UUID ownerUuid);
}
