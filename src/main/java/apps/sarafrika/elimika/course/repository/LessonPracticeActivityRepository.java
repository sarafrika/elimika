package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.LessonPracticeActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonPracticeActivityRepository extends JpaRepository<LessonPracticeActivity, Long>,
        JpaSpecificationExecutor<LessonPracticeActivity> {

    Optional<LessonPracticeActivity> findByUuid(UUID uuid);

    Optional<LessonPracticeActivity> findByUuidAndLessonUuid(UUID uuid, UUID lessonUuid);

    Page<LessonPracticeActivity> findByLessonUuid(UUID lessonUuid, Pageable pageable);

    List<LessonPracticeActivity> findByLessonUuidOrderByDisplayOrderAsc(UUID lessonUuid);

    @Query("select coalesce(max(activity.displayOrder), 0) from LessonPracticeActivity activity where activity.lessonUuid = :lessonUuid")
    int findMaxDisplayOrderByLessonUuid(@Param("lessonUuid") UUID lessonUuid);
}
