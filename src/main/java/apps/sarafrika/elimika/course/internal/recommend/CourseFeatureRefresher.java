package apps.sarafrika.elimika.course.internal.recommend;

import apps.sarafrika.elimika.shared.spi.MinorLearnerLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rewrites {@code course_learning_stats} and {@code course_co_enrolments} from {@code course_enrollments}
 * and {@code course_reviews}, in one transaction, so readers see either last night's tables or tonight's,
 * never a half-written mix.
 * <p>
 * Guarded by a transaction-scoped advisory lock ({@code pg_try_advisory_xact_lock}): a second instance, or
 * a manual run overlapping the schedule, skips instead of waiting, and the lock releases itself at commit or
 * rollback, so a crashed run can never leave it held on a pooled connection.
 * <p>
 * Minors: the co-enrolment threshold rises from {@value #MIN_SHARED_LEARNERS} to
 * {@value #MIN_SHARED_LEARNERS_WITH_MINORS} shared learners whenever any learner behind a pair is a minor.
 * Ages come from {@link MinorLearnerLookupService} (student maps to user, tenancy tests {@code users.dob}),
 * so this module never reads another module's tables. A missing date of birth counts as an adult.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseFeatureRefresher {

    /** Arbitrary but fixed: identifies this job's advisory lock across instances. */
    static final long LOCK_KEY = 0x0C0_FEA7_0001L;

    /** Bayesian prior weight: a course's rating counts as this many reviews at the global mean. */
    static final int RATING_PRIOR_WEIGHT = 5;

    static final int MIN_SHARED_LEARNERS = 5;
    static final int MIN_SHARED_LEARNERS_WITH_MINORS = 10;

    private static final int CHUNK = 1000;

    private final NamedParameterJdbcTemplate jdbc;
    private final MinorLearnerLookupService minorLearnerLookupService;

    public record Outcome(boolean ran, int statsRows, int coEnrolmentRows) {
        static Outcome skipped() {
            return new Outcome(false, 0, 0);
        }
    }

    @Transactional
    public Outcome refresh(Instant now) {
        Boolean locked = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(:key)",
                Map.of("key", LOCK_KEY), Boolean.class);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("Course feature refresh skipped: another run holds the lock");
            return Outcome.skipped();
        }

        OffsetDateTime computedAt = now.atOffset(ZoneOffset.UTC);
        int stats = rewriteLearningStats(computedAt);
        int pairs = rewriteCoEnrolments(computedAt);
        log.info("Course features refreshed: {} stats rows, {} co-enrolment rows", stats, pairs);
        return new Outcome(true, stats, pairs);
    }

    private int rewriteLearningStats(OffsetDateTime computedAt) {
        jdbc.update("DELETE FROM course_learning_stats", Map.of());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("now", computedAt)
                .addValue("priorWeight", RATING_PRIOR_WEIGHT);
        // One row per root course, enrolled or not, so every course has a rating_bayes (the prior when
        // it has no reviews). Shadow drafts are pending edits and get nothing.
        return jdbc.update("""
                WITH global_mean AS (
                    SELECT AVG(r.rating)::numeric AS m
                    FROM course_reviews r
                    JOIN courses c ON c.uuid = r.course_uuid AND c.parent_course_uuid IS NULL
                ),
                enrolments AS (
                    SELECT e.course_uuid,
                           COUNT(*) AS enrolment_count,
                           COUNT(*) FILTER (WHERE LOWER(e.status) = 'active') AS active_count,
                           COUNT(*) FILTER (WHERE LOWER(e.status) = 'completed') AS completed_count,
                           COUNT(*) FILTER (WHERE LOWER(e.status) = 'dropped') AS dropped_count,
                           AVG(COALESCE(e.progress_percentage, 0)) AS avg_progress,
                           PERCENTILE_CONT(0.5) WITHIN GROUP (
                               ORDER BY EXTRACT(EPOCH FROM (e.completion_date - e.enrollment_date)) / 86400.0
                           ) FILTER (WHERE LOWER(e.status) = 'completed'
                                     AND e.completion_date IS NOT NULL AND e.enrollment_date IS NOT NULL)
                               AS median_days_to_complete,
                           COUNT(*) FILTER (WHERE e.enrollment_date >= CAST(:now AS timestamptz) - INTERVAL '30 days')
                               AS enrolments_30d
                    FROM course_enrollments e
                    GROUP BY e.course_uuid
                ),
                reviews AS (
                    SELECT course_uuid, COUNT(*) AS n, SUM(rating) AS total
                    FROM course_reviews
                    GROUP BY course_uuid
                )
                INSERT INTO course_learning_stats (course_uuid, enrolment_count, active_count, completed_count,
                    dropped_count, completion_rate, avg_progress, median_days_to_complete, enrolments_30d,
                    rating_bayes, computed_at)
                SELECT c.uuid,
                       COALESCE(en.enrolment_count, 0),
                       COALESCE(en.active_count, 0),
                       COALESCE(en.completed_count, 0),
                       COALESCE(en.dropped_count, 0),
                       ROUND(en.completed_count::numeric / NULLIF(en.enrolment_count, 0), 4),
                       ROUND(en.avg_progress, 2),
                       ROUND(en.median_days_to_complete::numeric, 2),
                       COALESCE(en.enrolments_30d, 0),
                       ROUND((:priorWeight * g.m + COALESCE(rv.total, 0)) / (:priorWeight + COALESCE(rv.n, 0)), 4),
                       CAST(:now AS timestamptz)
                FROM courses c
                CROSS JOIN global_mean g
                LEFT JOIN enrolments en ON en.course_uuid = c.uuid
                LEFT JOIN reviews rv ON rv.course_uuid = c.uuid
                WHERE c.parent_course_uuid IS NULL
                """, params);
    }

    private int rewriteCoEnrolments(OffsetDateTime computedAt) {
        jdbc.update("DELETE FROM course_co_enrolments", Map.of());

        List<UUID> learners = jdbc.queryForList("""
                SELECT DISTINCT student_uuid FROM course_enrollments
                WHERE LOWER(status) IN ('active', 'completed')
                """, Map.of(), UUID.class);
        Set<UUID> minors = minorLearnerLookupService.findMinorStudentUuids(learners, computedAt.toLocalDate());

        jdbc.update("CREATE TEMPORARY TABLE IF NOT EXISTS tmp_co_enrolment_minors (student_uuid UUID PRIMARY KEY) "
                + "ON COMMIT DROP", Map.of());
        jdbc.update("DELETE FROM tmp_co_enrolment_minors", Map.of());
        List<UUID> minorList = new ArrayList<>(minors);
        for (int from = 0; from < minorList.size(); from += CHUNK) {
            List<UUID> chunk = minorList.subList(from, Math.min(from + CHUNK, minorList.size()));
            MapSqlParameterSource[] rows = chunk.stream()
                    .map(uuid -> new MapSqlParameterSource("uuid", uuid))
                    .toArray(MapSqlParameterSource[]::new);
            jdbc.batchUpdate("INSERT INTO tmp_co_enrolment_minors (student_uuid) VALUES (:uuid)", rows);
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("now", computedAt)
                .addValue("minShared", MIN_SHARED_LEARNERS)
                .addValue("minSharedWithMinors", MIN_SHARED_LEARNERS_WITH_MINORS);
        // Both directions of every pair are written, so "neighbours of X" is one index lookup.
        // jaccard = |A∩B| / |A∪B|; lift = |A∩B|·N / (|A|·|B|) over the N learners with any counted enrolment.
        return jdbc.update("""
                WITH e AS (
                    SELECT DISTINCT ce.student_uuid, ce.course_uuid
                    FROM course_enrollments ce
                    JOIN courses c ON c.uuid = ce.course_uuid AND c.parent_course_uuid IS NULL
                    WHERE LOWER(ce.status) IN ('active', 'completed')
                ),
                course_size AS (SELECT course_uuid, COUNT(*) AS n FROM e GROUP BY course_uuid),
                total AS (SELECT COUNT(DISTINCT student_uuid) AS n FROM e),
                pairs AS (
                    SELECT a.course_uuid, b.course_uuid AS neighbour_course_uuid,
                           COUNT(*) AS shared_learners,
                           BOOL_OR(m.student_uuid IS NOT NULL) AS includes_minors
                    FROM e a
                    JOIN e b ON b.student_uuid = a.student_uuid AND b.course_uuid <> a.course_uuid
                    LEFT JOIN tmp_co_enrolment_minors m ON m.student_uuid = a.student_uuid
                    GROUP BY a.course_uuid, b.course_uuid
                )
                INSERT INTO course_co_enrolments (course_uuid, neighbour_course_uuid, shared_learners, jaccard, lift,
                    includes_minors, computed_at)
                SELECT p.course_uuid, p.neighbour_course_uuid, p.shared_learners,
                       ROUND(p.shared_learners::numeric / (sa.n + sb.n - p.shared_learners), 5),
                       ROUND(p.shared_learners::numeric * t.n / (sa.n * sb.n), 4),
                       p.includes_minors,
                       CAST(:now AS timestamptz)
                FROM pairs p
                JOIN course_size sa ON sa.course_uuid = p.course_uuid
                JOIN course_size sb ON sb.course_uuid = p.neighbour_course_uuid
                CROSS JOIN total t
                WHERE p.shared_learners >= CASE WHEN p.includes_minors THEN :minSharedWithMinors ELSE :minShared END
                """, params);
    }
}
