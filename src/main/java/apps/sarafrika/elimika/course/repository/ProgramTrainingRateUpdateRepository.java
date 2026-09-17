package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramTrainingRateUpdate;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProgramTrainingRateUpdateRepository extends TrainingRateUpdateRepository<ProgramTrainingRateUpdate> {

    @Query("""
            SELECT rateUpdate FROM ProgramTrainingRateUpdate rateUpdate
            WHERE rateUpdate.applicationUuid IN (
                SELECT application.uuid FROM ProgramTrainingApplication application
                WHERE application.programUuid = :programUuid)
            """)
    Page<ProgramTrainingRateUpdate> findForProgram(@Param("programUuid") UUID programUuid, Pageable pageable);

    @Query("""
            SELECT rateUpdate FROM ProgramTrainingRateUpdate rateUpdate
            WHERE rateUpdate.status = :status
              AND rateUpdate.applicationUuid IN (
                SELECT application.uuid FROM ProgramTrainingApplication application
                WHERE application.programUuid = :programUuid)
            """)
    Page<ProgramTrainingRateUpdate> findForProgramWithStatus(@Param("programUuid") UUID programUuid,
                                                             @Param("status") TrainingRateUpdateStatus status,
                                                             Pageable pageable);
}
