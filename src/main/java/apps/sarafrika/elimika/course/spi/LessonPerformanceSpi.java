package apps.sarafrika.elimika.course.spi;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One lesson's marks for a set of learners, read for a session report. Grades stay owned by the
 * course module; callers get snapshots and may only finalise drafts through {@link #submitLessonDrafts}.
 */
public interface LessonPerformanceSpi {

    /** The lesson a session teaches: the one it names when it belongs to the course, else the one at its position. */
    Optional<LessonRef> resolveLesson(UUID courseUuid, UUID lessonUuid, Integer sessionNumber);

    LessonPerformance findLessonPerformance(UUID courseUuid, UUID lessonUuid, Collection<UUID> studentUuids);

    /** Turns every complete draft lesson score of these learners into a submitted score; returns how many. */
    int submitLessonDrafts(UUID courseUuid, UUID lessonUuid, Collection<UUID> studentUuids, UUID graderUserUuid);

    record LessonRef(UUID lessonUuid, Integer lessonNumber, String title) {
    }

    record LessonPerformance(
            LessonRef lesson,
            boolean rubricScored,
            BigDecimal maxScore,
            List<Criterion> criteria,
            List<Band> bands,
            List<String> assignmentTitles,
            List<StudentLesson> students
    ) {
    }

    record Criterion(UUID criteriaUuid, String name, Integer displayOrder) {
    }

    record Band(String name, BigDecimal minPercentage) {
    }

    /** status is none, draft or submitted; picks are empty for none. */
    record StudentLesson(
            UUID studentUuid,
            UUID enrollmentUuid,
            String status,
            BigDecimal score,
            BigDecimal maxScore,
            List<Pick> picks,
            String comments,
            int revisionCount,
            LocalDateTime gradedAt,
            List<AssignmentStatus> assignments
    ) {
    }

    record Pick(UUID criteriaUuid, String criteriaName, String levelName, BigDecimal points) {
    }

    record AssignmentStatus(String title, String status, BigDecimal score, BigDecimal maxScore) {
    }
}
