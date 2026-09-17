package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.TrainingApplicationVenue;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TrainingApplicationVenueRepository extends JpaRepository<TrainingApplicationVenue, Long> {

    List<TrainingApplicationVenue> findByApplicationTypeAndApplicationUuidInOrderByIdAsc(
            TrainingApplicationType applicationType, Collection<UUID> applicationUuids);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM TrainingApplicationVenue venue "
            + "WHERE venue.applicationType = :applicationType AND venue.applicationUuid = :applicationUuid")
    void deleteForApplication(@Param("applicationType") TrainingApplicationType applicationType,
                              @Param("applicationUuid") UUID applicationUuid);
}
