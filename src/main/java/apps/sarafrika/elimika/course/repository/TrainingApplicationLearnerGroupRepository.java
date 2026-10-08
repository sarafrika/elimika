package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.TrainingApplicationLearnerGroup;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TrainingApplicationLearnerGroupRepository extends JpaRepository<TrainingApplicationLearnerGroup, Long> {

    List<TrainingApplicationLearnerGroup> findByApplicationTypeAndApplicationUuidInOrderByPositionAscIdAsc(
            TrainingApplicationType applicationType, Collection<UUID> applicationUuids);

    /** Lesson hours go with their group through the foreign key's ON DELETE CASCADE. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM TrainingApplicationLearnerGroup learnerGroup "
            + "WHERE learnerGroup.applicationType = :applicationType AND learnerGroup.applicationUuid = :applicationUuid")
    void deleteForApplication(@Param("applicationType") TrainingApplicationType applicationType,
                              @Param("applicationUuid") UUID applicationUuid);
}
