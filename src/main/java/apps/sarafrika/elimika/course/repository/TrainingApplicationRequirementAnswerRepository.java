package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.TrainingApplicationRequirementAnswer;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TrainingApplicationRequirementAnswerRepository extends JpaRepository<TrainingApplicationRequirementAnswer, Long> {

    List<TrainingApplicationRequirementAnswer> findByApplicationTypeAndApplicationUuidInOrderByIdAsc(
            TrainingApplicationType applicationType, Collection<UUID> applicationUuids);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM TrainingApplicationRequirementAnswer answer "
            + "WHERE answer.applicationType = :applicationType AND answer.applicationUuid = :applicationUuid")
    void deleteForApplication(@Param("applicationType") TrainingApplicationType applicationType,
                              @Param("applicationUuid") UUID applicationUuid);
}
