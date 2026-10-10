package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.QuizAttempt;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long>, JpaSpecificationExecutor<QuizAttempt> {
    Optional<QuizAttempt> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    long countByEnrollmentUuidAndQuizUuid(UUID enrollmentUuid, UUID quizUuid);

    Optional<QuizAttempt> findTopByEnrollmentUuidAndQuizUuidOrderByAttemptNumberDesc(UUID enrollmentUuid, UUID quizUuid);

    /**
     * Which of these quizzes the learner has submitted an attempt at through any course enrolment.
     * An attempt still in progress does not count.
     */
    @Query("""
        SELECT DISTINCT a.quizUuid FROM QuizAttempt a
        WHERE a.quizUuid IN :quizUuids
        AND a.status <> :inProgress
        AND a.enrollmentUuid IN (SELECT ce.uuid FROM CourseEnrollment ce WHERE ce.studentUuid = :studentUuid)
        """)
    List<UUID> findSubmittedQuizUuids(@Param("studentUuid") UUID studentUuid,
                                      @Param("quizUuids") Collection<UUID> quizUuids,
                                      @Param("inProgress") AttemptStatus inProgress);
}
