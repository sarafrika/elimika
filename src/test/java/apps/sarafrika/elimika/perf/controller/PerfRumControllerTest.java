package apps.sarafrika.elimika.perf.controller;

import apps.sarafrika.elimika.perf.dto.RumIngestResponse;
import apps.sarafrika.elimika.perf.dto.RumSummaryResponse;
import apps.sarafrika.elimika.perf.dto.RumSummaryRow;
import apps.sarafrika.elimika.perf.internal.RumRateLimiter;
import apps.sarafrika.elimika.perf.service.PerfRumService;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {PerfRumController.class, PerfRumAdminController.class},
        properties = "app.keycloak.realm=test-realm")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Import(PerfRumControllerTest.MockConfig.class)
class PerfRumControllerTest {

    private static final String EVENT = """
            {"route_template":"/dashboard/overview","domain":"student","metric":"LCP","value_ms":1830.5,
             "network_type":"4g","device_class":"mobile","app_version":"1.0.0",
             "occurred_at":"2026-10-08T12:00:00Z"}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PerfRumService perfRumService;

    @Autowired
    private RumRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        reset(perfRumService, rateLimiter);
        when(rateLimiter.tryAcquire(any(), any())).thenReturn(true);
    }

    @Test
    void acceptsABatchFromAnAnonymousVisitor() throws Exception {
        when(perfRumService.ingest(anyList(), anyBoolean())).thenReturn(new RumIngestResponse(1, 0));

        mockMvc.perform(post("/api/v1/perf/rum").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[" + EVENT + "]}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.accepted").value(1));
        verify(perfRumService).ingest(anyList(), Mockito.eq(false));
    }

    @Test
    void rejectsMoreThanFiftyEvents() throws Exception {
        String events = IntStream.range(0, 51).mapToObj(i -> EVENT).collect(Collectors.joining(","));

        mockMvc.perform(post("/api/v1/perf/rum").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[" + events + "]}"))
                .andExpect(status().isBadRequest());
        verify(perfRumService, never()).ingest(anyList(), anyBoolean());
    }

    @Test
    void rejectsANegativeValue() throws Exception {
        mockMvc.perform(post("/api/v1/perf/rum").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[" + EVENT.replace("1830.5", "-1") + "]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answersTooManyRequestsOnceTheCallerIsOverItsLimit() throws Exception {
        when(rateLimiter.tryAcquire(any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/perf/rum").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[" + EVENT + "]}"))
                .andExpect(status().isTooManyRequests());
        verify(perfRumService, never()).ingest(anyList(), anyBoolean());
    }

    @Test
    void summaryReturnsPercentilesInSnakeCase() throws Exception {
        OffsetDateTime to = OffsetDateTime.parse("2026-10-08T12:00:00Z");
        when(perfRumService.summarize(any(), any())).thenReturn(new RumSummaryResponse(to.minusDays(1), to,
                List.of(new RumSummaryRow("/dashboard/overview", "LCP", 10, 900.0, 2400.0, 3100.0))));

        mockMvc.perform(get("/api/v1/admin/perf/rum/summary")
                        .param("from", "2026-10-07T12:00:00Z").param("to", "2026-10-08T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].route_template").value("/dashboard/overview"))
                .andExpect(jsonPath("$.data.rows[0].p95_ms").value(2400.0));
    }

    static class MockConfig {
        @Bean
        PerfRumService perfRumService() {
            return Mockito.mock(PerfRumService.class);
        }

        @Bean
        RumRateLimiter rumRateLimiter() {
            return Mockito.mock(RumRateLimiter.class);
        }

        @Bean
        UserManagementService userManagementService() {
            return Mockito.mock(UserManagementService.class);
        }

        @Bean
        RequestAuditService requestAuditService() {
            return Mockito.mock(RequestAuditService.class);
        }
    }
}
