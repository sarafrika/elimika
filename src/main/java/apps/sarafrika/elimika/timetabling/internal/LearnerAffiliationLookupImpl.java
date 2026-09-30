package apps.sarafrika.elimika.timetabling.internal;

import apps.sarafrika.elimika.shared.spi.enrollment.LearnerAffiliationLookup;
import apps.sarafrika.elimika.shared.spi.enrollment.LearnerAffiliations;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Answers a learner's affiliations from their class enrolments (one query) plus the organisation
 * memberships they still hold (through tenancy). Cancelled and waitlisted enrolments never taught anyone,
 * so they do not count.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearnerAffiliationLookupImpl implements LearnerAffiliationLookup {

    private final NamedParameterJdbcTemplate jdbc;
    private final StudentLookupService studentLookupService;
    private final UserLookupService userLookupService;

    @Override
    public LearnerAffiliations findAffiliations(UUID studentUuid) {
        if (studentUuid == null) {
            return LearnerAffiliations.none();
        }
        Set<UUID> organisations = new HashSet<>();
        Set<UUID> instructors = new HashSet<>();
        Set<UUID> courses = new HashSet<>();
        jdbc.query("""
                SELECT DISTINCT cd.organisation_uuid, si.instructor_uuid, cd.default_instructor_uuid, cd.course_uuid
                FROM class_enrollments ce
                JOIN scheduled_instances si ON si.uuid = ce.scheduled_instance_uuid
                LEFT JOIN class_definitions cd ON cd.uuid = si.class_definition_uuid
                WHERE ce.student_uuid = :studentUuid
                  AND UPPER(ce.status) NOT IN ('CANCELLED', 'WAITLISTED')
                """, new MapSqlParameterSource("studentUuid", studentUuid), rs -> {
            addIfPresent(organisations, rs.getObject("organisation_uuid", UUID.class));
            addIfPresent(instructors, rs.getObject("instructor_uuid", UUID.class));
            addIfPresent(instructors, rs.getObject("default_instructor_uuid", UUID.class));
            addIfPresent(courses, rs.getObject("course_uuid", UUID.class));
        });
        studentLookupService.getStudentUserUuid(studentUuid)
                .map(userLookupService::getActiveUserOrganizations)
                .ifPresent(memberships -> memberships.stream().filter(java.util.Objects::nonNull).forEach(organisations::add));
        return new LearnerAffiliations(organisations, instructors, courses);
    }

    private static void addIfPresent(Set<UUID> target, UUID value) {
        if (value != null) {
            target.add(value);
        }
    }
}
