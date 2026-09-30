package apps.sarafrika.elimika.student.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Reads and writes {@code learner_skill_goals}. */
@Component
@RequiredArgsConstructor
public class LearnerSkillGoalStore {

    public static final String SOURCE_SELF = "SELF";

    private final NamedParameterJdbcTemplate jdbc;

    /** One stored goal. */
    public record Row(UUID skillUuid, String source, LocalDateTime createdDate) {
    }

    public List<Row> findByStudent(UUID studentUuid) {
        return jdbc.query("""
                        SELECT skill_uuid, source, created_date FROM learner_skill_goals
                        WHERE student_uuid = :studentUuid ORDER BY created_date, id
                        """, new MapSqlParameterSource("studentUuid", studentUuid),
                (rs, rowNum) -> new Row(rs.getObject("skill_uuid", UUID.class), rs.getString("source"),
                        rs.getObject("created_date", LocalDateTime.class)));
    }

    public List<UUID> findSkillUuids(UUID studentUuid) {
        return jdbc.queryForList("""
                SELECT skill_uuid FROM learner_skill_goals
                WHERE student_uuid = :studentUuid ORDER BY created_date, id
                """, new MapSqlParameterSource("studentUuid", studentUuid), UUID.class);
    }

    /** Removes goals not in {@code keep} and adds the missing ones, so kept goals keep their dates. */
    public void replace(UUID studentUuid, List<UUID> keep, String source, String actor) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("studentUuid", studentUuid)
                .addValue("source", source)
                .addValue("actor", actor);
        if (keep.isEmpty()) {
            jdbc.update("DELETE FROM learner_skill_goals WHERE student_uuid = :studentUuid", params);
            return;
        }
        params.addValue("keep", keep);
        jdbc.update("DELETE FROM learner_skill_goals WHERE student_uuid = :studentUuid AND skill_uuid NOT IN (:keep)",
                params);
        MapSqlParameterSource[] rows = keep.stream()
                .map(skillUuid -> new MapSqlParameterSource()
                        .addValue("studentUuid", studentUuid)
                        .addValue("skillUuid", skillUuid)
                        .addValue("source", source)
                        .addValue("actor", actor))
                .toArray(MapSqlParameterSource[]::new);
        jdbc.batchUpdate("""
                INSERT INTO learner_skill_goals (student_uuid, skill_uuid, source, created_by)
                VALUES (:studentUuid, :skillUuid, :source, :actor)
                ON CONFLICT (student_uuid, skill_uuid) DO NOTHING
                """, rows);
    }
}
