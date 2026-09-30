package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorReview;
import apps.sarafrika.elimika.instructor.search.InstructorRatingAggregate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InstructorReviewRepository extends JpaRepository<InstructorReview, Long> {

    Optional<InstructorReview> findByUuid(UUID uuid);

    boolean existsByInstructorUuidAndEnrollmentUuid(UUID instructorUuid, UUID enrollmentUuid);

    List<InstructorReview> findByInstructorUuid(UUID instructorUuid);

    @Query("SELECT AVG(r.rating) FROM InstructorReview r WHERE r.instructorUuid = :instructorUuid")
    Double findAverageRatingForInstructor(@Param("instructorUuid") UUID instructorUuid);

    /** The platform-wide mean rating, the prior of every instructor's Bayesian average. */
    @Query("SELECT AVG(r.rating) FROM InstructorReview r WHERE r.rating IS NOT NULL")
    Double findPlatformAverageRating();

    /** Average rating and review count per instructor, in one query, for search documents. */
    @Query("""
            SELECT new apps.sarafrika.elimika.instructor.search.InstructorRatingAggregate(
                       r.instructorUuid, AVG(r.rating), COUNT(r))
            FROM InstructorReview r
            WHERE r.instructorUuid IN :instructorUuids
            GROUP BY r.instructorUuid
            """)
    List<InstructorRatingAggregate> aggregateRatingsByInstructorUuidIn(
            @Param("instructorUuids") Collection<UUID> instructorUuids);
}
