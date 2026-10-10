package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseReview;
import apps.sarafrika.elimika.course.repository.projection.CourseRatingAggregateView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    List<CourseReview> findByCourseUuid(UUID courseUuid);

    Optional<CourseReview> findByCourseUuidAndStudentUuid(UUID courseUuid, UUID studentUuid);

    boolean existsByCourseUuidAndStudentUuid(UUID courseUuid, UUID studentUuid);

    /** Rating aggregates for a page of courses in one grouped query; unreviewed courses are absent. */
    @Query("""
            SELECT new apps.sarafrika.elimika.course.repository.projection.CourseRatingAggregateView(
                       r.courseUuid, AVG(r.rating), COUNT(r))
            FROM CourseReview r
            WHERE r.courseUuid IN :courseUuids
            GROUP BY r.courseUuid
            """)
    List<CourseRatingAggregateView> aggregateByCourseUuidIn(@Param("courseUuids") Collection<UUID> courseUuids);
}
