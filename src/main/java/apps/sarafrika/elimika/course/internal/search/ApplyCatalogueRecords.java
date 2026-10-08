package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.dto.ApplyCatalogueApplication;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The database reads behind the apply-to-train catalogue: the caller's own applications, the
 * public-visibility re-check of a page of hits with their minimum training fees, and category names.
 */
@Component
public class ApplyCatalogueRecords {

    private final NamedParameterJdbcTemplate jdbc;

    public ApplyCatalogueRecords(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The instructor's course applications keyed by course UUID. */
    public Map<UUID, ApplyCatalogueApplication> courseApplications(UUID instructorUuid) {
        return applications("course_training_applications", "course_uuid", instructorUuid);
    }

    /** The instructor's programme applications keyed by program UUID. */
    public Map<UUID, ApplyCatalogueApplication> programApplications(UUID instructorUuid) {
        return applications("program_training_applications", "program_uuid", instructorUuid);
    }

    private Map<UUID, ApplyCatalogueApplication> applications(String table, String targetColumn, UUID instructorUuid) {
        if (instructorUuid == null) {
            return Map.of();
        }
        Map<UUID, ApplyCatalogueApplication> byTarget = new HashMap<>();
        jdbc.query("SELECT uuid, " + targetColumn + " AS target_uuid, status FROM " + table
                        + " WHERE applicant_uuid = :applicant AND UPPER(applicant_type) = 'INSTRUCTOR'",
                new MapSqlParameterSource("applicant", instructorUuid), rs -> {
                    String status = rs.getString("status");
                    byTarget.put(SearchRows.uuid(rs, "target_uuid"), new ApplyCatalogueApplication(
                            SearchRows.uuid(rs, "uuid"), status == null ? null : status.toLowerCase(Locale.ROOT)));
                });
        return byTarget;
    }

    /**
     * The public re-check for courses (root, published, admin-approved, active) with each survivor's
     * minimum training fee, zero when unset.
     */
    public Map<UUID, BigDecimal> publicCourseFees(Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BigDecimal> fees = new HashMap<>();
        jdbc.query("""
                SELECT c.uuid, COALESCE(c.minimum_training_fee, 0) AS fee
                FROM courses c
                WHERE c.uuid IN (:uuids)
                  AND c.parent_course_uuid IS NULL
                  AND LOWER(c.status) = 'published' AND c.admin_approved = true AND c.active = true
                """, new MapSqlParameterSource("uuids", uuids),
                rs -> {
                    fees.put(SearchRows.uuid(rs, "uuid"), rs.getBigDecimal("fee"));
                });
        return fees;
    }

    /**
     * The public re-check for programmes (published, admin-approved, active) with each survivor's
     * floor: the highest minimum fee across its member courses, as {@code TrainingFeeFloors} holds it.
     */
    public Map<UUID, BigDecimal> publicProgramFees(Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BigDecimal> fees = new HashMap<>();
        jdbc.query("""
                SELECT p.uuid,
                       COALESCE((SELECT MAX(c.minimum_training_fee) FROM program_courses pc
                                 JOIN courses c ON c.uuid = pc.course_uuid
                                 WHERE pc.program_uuid = p.uuid), 0) AS fee
                FROM training_programs p
                WHERE p.uuid IN (:uuids)
                  AND LOWER(p.status) = 'published' AND p.admin_approved = true AND p.is_active = true
                """, new MapSqlParameterSource("uuids", uuids),
                rs -> {
                    fees.put(SearchRows.uuid(rs, "uuid"), rs.getBigDecimal("fee"));
                });
        return fees;
    }

    public Map<UUID, String> categoryNames(Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        jdbc.query("SELECT uuid, name FROM course_categories WHERE uuid IN (:uuids)",
                new MapSqlParameterSource("uuids", uuids),
                rs -> {
                    names.put(SearchRows.uuid(rs, "uuid"), rs.getString("name"));
                });
        return names;
    }
}
