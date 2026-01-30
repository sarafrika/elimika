package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseEnrollmentBackfillService {

    private static final String CLASS_ENROLLMENT_COURSE_QUERY = """
            SELECT DISTINCT ce.student_uuid, cd.course_uuid
            FROM class_enrollments ce
            JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid
            JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid
            WHERE cd.course_uuid IS NOT NULL
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final CourseEnrollmentSyncService courseEnrollmentSyncService;

    public int backfillCourseEnrollments() {
        int processed = 0;

        List<ProgramEnrollment> programEnrollments = programEnrollmentRepository.findAll();
        for (ProgramEnrollment enrollment : programEnrollments) {
            courseEnrollmentSyncService.syncFromProgramEnrollment(
                    enrollment.getStudentUuid(),
                    enrollment.getProgramUuid(),
                    enrollment.getStatus()
            );
            processed++;
        }

        List<CourseEnrollmentCandidate> classEnrollmentCourses =
                jdbcTemplate.query(CLASS_ENROLLMENT_COURSE_QUERY, new CourseEnrollmentCandidateMapper());
        for (CourseEnrollmentCandidate candidate : classEnrollmentCourses) {
            courseEnrollmentSyncService.syncFromClassEnrollments(candidate.studentUuid(), candidate.courseUuid());
            processed++;
        }

        log.info("Course enrollment backfill processed {} enrollment relationships", processed);
        return processed;
    }

    private record CourseEnrollmentCandidate(UUID studentUuid, UUID courseUuid) { }

    private static class CourseEnrollmentCandidateMapper implements RowMapper<CourseEnrollmentCandidate> {
        @Override
        public CourseEnrollmentCandidate mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CourseEnrollmentCandidate(
                    UUID.fromString(rs.getString("student_uuid")),
                    UUID.fromString(rs.getString("course_uuid"))
            );
        }
    }
}
