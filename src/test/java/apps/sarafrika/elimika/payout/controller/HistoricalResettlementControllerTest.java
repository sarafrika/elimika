package apps.sarafrika.elimika.payout.controller;

import apps.sarafrika.elimika.payout.service.HistoricalResettlementService;
import apps.sarafrika.elimika.payout.service.HistoricalResettlementService.Report;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HistoricalResettlementControllerTest {

    @Mock
    private HistoricalResettlementService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HistoricalResettlementController(service)).build();
    }

    @Test
    void isADryRunUnlessAskedToApply() throws Exception {
        when(service.run(false)).thenReturn(new Report(false, List.of(), BigDecimal.ZERO, BigDecimal.ZERO));

        mockMvc.perform(post("/api/v1/admin/payouts/historical-resettlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applied").value(false));

        verify(service).run(false);
    }

    @Test
    void isForPlatformAdminsOnly() {
        assertThat(HistoricalResettlementController.class.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("@domainSecurityService.isPlatformAdmin()");
    }
}
