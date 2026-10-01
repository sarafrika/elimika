package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Guardian link JSON contract")
class GuardianStudentLinkJsonTest {

    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID GUARDIAN = UUID.randomUUID();

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("The request reads snake_case")
    void requestReadsSnakeCase() throws Exception {
        GuardianStudentLinkRequest request = objectMapper.readValue("""
                {"student_uuid":"%s","guardian_user_uuid":"%s","relationship_type":"PARENT",
                 "share_scope":"FULL","is_primary":true,"notes":"hi"}
                """.formatted(STUDENT, GUARDIAN), GuardianStudentLinkRequest.class);

        assertRequest(request);
    }

    @Test
    @DisplayName("The request still reads the old camelCase names for one release")
    void requestStillReadsCamelCase() throws Exception {
        GuardianStudentLinkRequest request = objectMapper.readValue("""
                {"studentUuid":"%s","guardianUserUuid":"%s","relationshipType":"PARENT",
                 "shareScope":"FULL","isPrimary":true,"notes":"hi"}
                """.formatted(STUDENT, GUARDIAN), GuardianStudentLinkRequest.class);

        assertRequest(request);
    }

    @Test
    @DisplayName("The response is snake_case only")
    void responseIsSnakeCaseOnly() throws Exception {
        GuardianStudentLinkDTO dto = new GuardianStudentLinkDTO(UUID.randomUUID(), STUDENT, GUARDIAN, "Kid",
                "Parent", GuardianRelationshipType.PARENT, GuardianShareScope.FULL, GuardianLinkStatus.ACTIVE,
                true, LocalDateTime.of(2026, 10, 1, 9, 0), null, "hi");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertThat(json.get("student_uuid").asText()).isEqualTo(STUDENT.toString());
        assertThat(json.get("guardian_user_uuid").asText()).isEqualTo(GUARDIAN.toString());
        assertThat(json.get("primary_guardian").asBoolean()).isTrue();
        assertThat(json.has("relationship_type")).isTrue();
        assertThat(json.has("share_scope")).isTrue();
        assertThat(json.has("linked_date")).isTrue();
        assertThat(json.has("studentUuid")).isFalse();
        assertThat(json.has("guardianUserUuid")).isFalse();
        assertThat(json.has("primaryGuardian")).isFalse();
    }

    private static void assertRequest(GuardianStudentLinkRequest request) {
        assertThat(request.studentUuid()).isEqualTo(STUDENT);
        assertThat(request.guardianUserUuid()).isEqualTo(GUARDIAN);
        assertThat(request.relationshipType()).isEqualTo(GuardianRelationshipType.PARENT);
        assertThat(request.shareScope()).isEqualTo(GuardianShareScope.FULL);
        assertThat(request.isPrimary()).isTrue();
        assertThat(request.notes()).isEqualTo("hi");
    }
}
