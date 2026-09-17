package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseTrainingRateUpdate;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CourseTrainingRateUpdateRepository extends TrainingRateUpdateRepository<CourseTrainingRateUpdate> {

    @Query("""
            SELECT rateUpdate FROM CourseTrainingRateUpdate rateUpdate
            WHERE rateUpdate.applicationUuid IN (
                SELECT application.uuid FROM CourseTrainingApplication application
                WHERE application.courseUuid = :courseUuid)
            """)
    Page<CourseTrainingRateUpdate> findForCourse(@Param("courseUuid") UUID courseUuid, Pageable pageable);

    @Query("""
            SELECT rateUpdate FROM CourseTrainingRateUpdate rateUpdate
            WHERE rateUpdate.status = :status
              AND rateUpdate.applicationUuid IN (
                SELECT application.uuid FROM CourseTrainingApplication application
                WHERE application.courseUuid = :courseUuid)
            """)
    Page<CourseTrainingRateUpdate> findForCourseWithStatus(@Param("courseUuid") UUID courseUuid,
                                                           @Param("status") TrainingRateUpdateStatus status,
                                                           Pageable pageable);
}
