package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorReview;
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
}
