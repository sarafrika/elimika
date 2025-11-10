package apps.sarafrika.elimika.systemconfig.controller;

import apps.sarafrika.elimika.systemconfig.dto.SystemRuleRequest;
import apps.sarafrika.elimika.systemconfig.dto.SystemRuleResponse;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleScope;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import apps.sarafrika.elimika.systemconfig.enums.RuleValueType;
import apps.sarafrika.elimika.systemconfig.service.SystemRuleAdminService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemRuleAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemRuleAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SystemRuleAdminService systemRuleAdminService;

    @MockBean
    private UserManagementService userManagementService;

    @MockBean
    private RequestAuditService requestAuditService;

    @Test
    void listRulesReturnsPagedResponse() throws Exception {
        SystemRuleResponse response = sampleResponse();
        Page<SystemRuleResponse> page = new PageImpl<>(List.of(response));
        when(systemRuleAdminService.listRules(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/system-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].key").value("student.onboarding.age_gate"));
    }

    @Test
    void createRuleReturnsCreated() throws Exception {
        SystemRuleResponse response = sampleResponse();
        when(systemRuleAdminService.createRule(any(SystemRuleRequest.class))).thenReturn(response);

        JsonNode payload = objectMapper.readTree("{\"minAge\":5,\"maxAge\":18}");
        SystemRuleRequest request = new SystemRuleRequest(
                RuleCategory.AGE_GATE,
                "student.onboarding.age_gate",
                RuleScope.GLOBAL,
                null,
                0,
                RuleStatus.ACTIVE,
                RuleValueType.JSON,
                payload,
                null,
                OffsetDateTime.now(ZoneOffset.UTC),
                null
        );

        mockMvc.perform(post("/api/v1/system-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.key").value("student.onboarding.age_gate"));
    }

    private SystemRuleResponse sampleResponse() {
        return new SystemRuleResponse(
                UUID.randomUUID(),
                RuleCategory.AGE_GATE,
                "student.onboarding.age_gate",
                RuleScope.GLOBAL,
                null,
                0,
                RuleStatus.ACTIVE,
                RuleValueType.JSON,
                objectMapper.createObjectNode(),
                null,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                LocalDateTime.now(),
                "system",
                LocalDateTime.now(),
                "system"
        );
    }
}
