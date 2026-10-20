package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgramProgressView;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The parent dashboard reads these in snake_case; camelCase left every parent with "no learners linked". */
class GuardianJsonContractTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("the guardian's learner list and dashboard serialise in snake_case")
    void guardianPayloadsAreSnakeCase() {
        JsonNode summary = mapper.valueToTree(new GuardianStudentSummaryDTO(UUID.randomUUID(), UUID.randomUUID(), "Sam",
                GuardianRelationshipType.values()[0], GuardianShareScope.values()[0], GuardianLinkStatus.values()[0], true));
        assertThat(fieldNames(summary)).containsExactlyInAnyOrder(
                "link_uuid", "student_uuid", "student_name", "relationship_type", "share_scope", "status", "primary_guardian");

        LearnerCourseProgressView course = new LearnerCourseProgressView(UUID.randomUUID(), UUID.randomUUID(), "JS",
                "active", BigDecimal.TEN, LocalDateTime.of(2026, 10, 20, 9, 0));
        LearnerProgramProgressView program = new LearnerProgramProgressView(UUID.randomUUID(), UUID.randomUUID(), "Web",
                "active", BigDecimal.ONE, LocalDateTime.of(2026, 10, 20, 9, 0));
        JsonNode dashboard = mapper.valueToTree(new GuardianStudentDashboardDTO(UUID.randomUUID(), "Sam",
                GuardianShareScope.values()[0], GuardianLinkStatus.values()[0], List.of(course), List.of(program)));
        assertThat(fieldNames(dashboard)).containsExactlyInAnyOrder(
                "student_uuid", "student_name", "share_scope", "status", "course_progress", "program_progress");
        assertThat(fieldNames(dashboard.get("course_progress").get(0))).containsExactlyInAnyOrder(
                "enrollment_uuid", "course_uuid", "course_name", "status", "progress_percentage", "updated_date");
        assertThat(fieldNames(dashboard.get("program_progress").get(0))).containsExactlyInAnyOrder(
                "enrollment_uuid", "program_uuid", "program_name", "status", "progress_percentage", "updated_date");
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
