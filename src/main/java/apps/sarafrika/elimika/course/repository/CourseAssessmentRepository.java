package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseAssessmentRepository extends JpaRepository<CourseAssessment, Long>,
        JpaSpecificationExecutor<CourseAssessment> {
    Optional<CourseAssessment> findByUuid(UUID uuid);

    Optional<CourseAssessment> findByUuidAndCourseUuid(UUID uuid, UUID courseUuid);

    List<CourseAssessment> findByCourseUuidOrderByCreatedDateAsc(UUID courseUuid);

    List<CourseAssessment> findByCourseUuidAndSyncClassAttendanceTrueOrderByCreatedDateAsc(UUID courseUuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    @Query("""
            SELECT COALESCE(SUM(ca.weightPercentage), 0)
            FROM CourseAssessment ca
            WHERE ca.courseUuid = :courseUuid
            AND (:excludedAssessmentUuid IS NULL OR ca.uuid <> :excludedAssessmentUuid)
            """)
    BigDecimal sumWeightPercentageByCourseUuidExcluding(
            @Param("courseUuid") UUID courseUuid,
            @Param("excludedAssessmentUuid") UUID excludedAssessmentUuid
    );
}
