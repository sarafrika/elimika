package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    List<CourseReview> findByCourseUuid(UUID courseUuid);

    Optional<CourseReview> findByCourseUuidAndStudentUuid(UUID courseUuid, UUID studentUuid);

    boolean existsByCourseUuidAndStudentUuid(UUID courseUuid, UUID studentUuid);
}
