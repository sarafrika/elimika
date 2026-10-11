package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.factory.LessonFactory;
import apps.sarafrika.elimika.course.internal.LearnerAssessmentScope;
import apps.sarafrika.elimika.course.internal.security.LearnerContentAccess;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// The lessons of many courses in one query, each course admitted by the same rule as
// GET /courses/{courseUuid}/lessons; courses the caller may not read are omitted, not refused.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseLessonBatchService {

    /** Upper bound on course ids per request, matching the other batch reads. */
    public static final int MAX_BATCH_SIZE = 100;

    private final LessonRepository lessonRepository;
    private final LearnerContentAccess learnerContentAccess;
    private final CourseSecuritySpi courseSecurityService;
    private final LearnerAssessmentScope learnerAssessmentScope;

    /** Lessons of the readable courses among {@code courseUuids}, by course then lesson number. */
    public List<LessonDTO> getLessons(Collection<UUID> courseUuids) {
        Set<UUID> ids = courseUuids == null ? Set.of() : courseUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("At least one course uuid must be requested");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("At most " + MAX_BATCH_SIZE + " course uuids may be requested at once");
        }
        boolean staff = learnerContentAccess.isStaff();
        List<UUID> readable = ids.stream()
                .filter(courseUuid -> staff || courseSecurityService.canReadCourseAsLearner(courseUuid))
                .toList();
        if (readable.isEmpty()) {
            return List.of();
        }
        log.debug("Batch lesson read for {} courses", readable.size());

        Specification<Lesson> spec = (root, query, cb) -> root.get("courseUuid").in(readable);
        if (!learnerAssessmentScope.seesAllLearners()) {
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.equal(root.get("status"), ContentStatus.PUBLISHED),
                    cb.isTrue(root.get("active"))));
        }
        return lessonRepository.findAll(spec, Sort.by("courseUuid", "lessonNumber")).stream()
                .map(LessonFactory::toDTO)
                .toList();
    }
}
