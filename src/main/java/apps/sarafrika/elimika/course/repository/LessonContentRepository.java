package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.LessonContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonContentRepository extends JpaRepository<LessonContent, Long>, JpaSpecificationExecutor<LessonContent> {
    Optional<LessonContent> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    long countByContentTypeUuid(UUID contentTypeUuid);

    boolean existsByUuid(UUID uuid);

    List<LessonContent> findByLessonUuidOrderByDisplayOrderAsc(UUID lessonUuid);

    List<LessonContent> findByLessonUuidAndIsRequiredTrueOrderByDisplayOrderAsc(UUID lessonUuid);

    List<LessonContent> findByLessonUuidAndIsRequiredFalseOrderByDisplayOrderAsc(UUID lessonUuid);

    List<LessonContent> findByLessonUuid(UUID lessonUuid);

    int findMaxDisplayOrderByLessonUuid(UUID lessonUuid);

    List<LessonContent> findByLessonUuidAndFileSizeBytesGreaterThan(UUID lessonUuid, long sizeThresholdBytes);

    long countByLessonUuid(UUID lessonUuid);

    long countByLessonUuidAndIsRequiredTrue(UUID lessonUuid);
}