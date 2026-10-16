package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseGradeBand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CourseGradeBandRepository extends JpaRepository<CourseGradeBand, Long> {

    List<CourseGradeBand> findByCourseUuidOrderByMinPercentageDesc(UUID courseUuid);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CourseGradeBand b WHERE b.courseUuid = :courseUuid")
    void deleteAllByCourseUuid(@Param("courseUuid") UUID courseUuid);
}
