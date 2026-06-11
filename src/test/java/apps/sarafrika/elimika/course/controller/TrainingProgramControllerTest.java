package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.ProgramRatingSummaryDTO;
import apps.sarafrika.elimika.course.dto.ProgramReviewDTO;
import apps.sarafrika.elimika.course.dto.ProgramReviewRequest;
import apps.sarafrika.elimika.course.service.CertificateService;
import apps.sarafrika.elimika.course.service.ProgramCourseService;
import apps.sarafrika.elimika.course.service.ProgramEnrollmentService;
import apps.sarafrika.elimika.course.service.ProgramRequirementService;
import apps.sarafrika.elimika.course.service.ProgramReviewService;
import apps.sarafrika.elimika.course.service.ProgramTrainingApplicationService;
import apps.sarafrika.elimika.course.service.TrainingProgramService;
import apps.sarafrika.elimika.shared.config.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingProgramControllerTest {

    @Mock
    private TrainingProgramService trainingProgramService;
    @Mock
    private ProgramCourseService programCourseService;
    @Mock
    private ProgramEnrollmentService programEnrollmentService;
    @Mock
    private ProgramRequirementService programRequirementService;
    @Mock
    private CertificateService certificateService;
    @Mock
    private ProgramTrainingApplicationService programTrainingApplicationService;
    @Mock
    private ProgramReviewService programReviewService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TrainingProgramController controller = new TrainingProgramController(
                trainingProgramService,
                programCourseService,
                programEnrollmentService,
                programRequirementService,
                certificateService,
                programTrainingApplicationService,
                programReviewService
        );

        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void submitProgramReviewUsesPathProgramUuidAndReturnsCreated() throws Exception {
        UUID programUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        ProgramReviewRequest request = request(studentUuid);
        ProgramReviewDTO response = review(programUuid, studentUuid, 5);

        when(programReviewService.saveProgramReview(eq(programUuid), any(ProgramReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/programs/{programUuid}/reviews", programUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.program_uuid").value(programUuid.toString()))
                .andExpect(jsonPath("$.data.student_uuid").value(studentUuid.toString()))
                .andExpect(jsonPath("$.data.rating").value(5));

        ArgumentCaptor<ProgramReviewRequest> captor = ArgumentCaptor.forClass(ProgramReviewRequest.class);
        verify(programReviewService).saveProgramReview(eq(programUuid), captor.capture());
        assertThat(captor.getValue().studentUuid()).isEqualTo(studentUuid);
    }

    @Test
    void getProgramReviewsReturnsPagedResponse() throws Exception {
        UUID programUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);

        when(programReviewService.getReviewsForProgram(eq(programUuid), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review(programUuid, studentUuid, 4)), pageable, 1));

        mockMvc.perform(get("/api/v1/programs/{programUuid}/reviews?page=0&size=20", programUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].program_uuid").value(programUuid.toString()))
                .andExpect(jsonPath("$.data.content[0].rating").value(4))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(1));
    }

    @Test
    void getProgramRatingSummaryReturnsAverageAndCount() throws Exception {
        UUID programUuid = UUID.randomUUID();

        when(programReviewService.getRatingSummary(programUuid))
                .thenReturn(new ProgramRatingSummaryDTO(programUuid, 4.5, 2L));

        mockMvc.perform(get("/api/v1/programs/{programUuid}/reviews/summary", programUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.program_uuid").value(programUuid.toString()))
                .andExpect(jsonPath("$.data.average_rating").value(4.5))
                .andExpect(jsonPath("$.data.review_count").value(2));
    }

    private ProgramReviewRequest request(UUID studentUuid) {
        return new ProgramReviewRequest(
                studentUuid,
                5,
                "Excellent pathway",
                "The sequence was practical.",
                false
        );
    }

    private ProgramReviewDTO review(UUID programUuid, UUID studentUuid, int rating) {
        return new ProgramReviewDTO(
                UUID.randomUUID(),
                programUuid,
                studentUuid,
                rating,
                "Excellent pathway",
                "The sequence was practical.",
                false,
                null,
                null,
                null,
                null
        );
    }
}
