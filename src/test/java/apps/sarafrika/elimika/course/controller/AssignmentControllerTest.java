package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.AssignmentDTO;
import apps.sarafrika.elimika.course.dto.AssignmentSubmissionAttachmentDTO;
import apps.sarafrika.elimika.course.dto.AssignmentSubmissionDTO;
import apps.sarafrika.elimika.course.dto.AssignmentSubmissionRequest;
import apps.sarafrika.elimika.course.internal.AssignmentMediaValidationService;
import apps.sarafrika.elimika.course.service.AssignmentAttachmentService;
import apps.sarafrika.elimika.course.service.AssignmentService;
import apps.sarafrika.elimika.course.service.AssignmentSubmissionAttachmentService;
import apps.sarafrika.elimika.course.service.AssignmentSubmissionService;
import apps.sarafrika.elimika.course.util.enums.SubmissionStatus;
import apps.sarafrika.elimika.shared.config.GlobalExceptionHandler;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AssignmentControllerTest {

    @Mock
    private AssignmentService assignmentService;
    @Mock
    private AssignmentSubmissionService assignmentSubmissionService;
    @Mock
    private AssignmentAttachmentService assignmentAttachmentService;
    @Mock
    private AssignmentSubmissionAttachmentService assignmentSubmissionAttachmentService;
    @Mock
    private AssignmentMediaValidationService assignmentMediaValidationService;
    @Mock
    private StorageService storageService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        AssignmentController controller = new AssignmentController(
                assignmentService,
                assignmentSubmissionService,
                assignmentAttachmentService,
                assignmentSubmissionAttachmentService,
                assignmentMediaValidationService,
                storageService,
                storageProperties
        );

        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void submitAssignmentAcceptsSnakeCaseJsonRequest() throws Exception {
        UUID assignmentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        AssignmentSubmissionRequest request = new AssignmentSubmissionRequest(
                enrollmentUuid,
                null,
                "My submission",
                new String[]{"https://example.test/submission.pdf"}
        );

        when(assignmentSubmissionService.submitAssignment(eq(assignmentUuid), any(AssignmentSubmissionRequest.class), eq(false)))
                .thenReturn(submission(UUID.randomUUID(), enrollmentUuid, assignmentUuid));

        mockMvc.perform(post("/api/v1/assignments/{assignmentUuid}/submit", assignmentUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enrollment_uuid").value(enrollmentUuid.toString()))
                .andExpect(jsonPath("$.data.assignment_uuid").value(assignmentUuid.toString()))
                .andExpect(jsonPath("$.data.status").value("submitted"));

        ArgumentCaptor<AssignmentSubmissionRequest> captor =
                ArgumentCaptor.forClass(AssignmentSubmissionRequest.class);
        verify(assignmentSubmissionService).submitAssignment(eq(assignmentUuid), captor.capture(), eq(false));
        assertThat(captor.getValue().submissionText()).isEqualTo("My submission");
        assertThat(captor.getValue().fileUrls()).containsExactly("https://example.test/submission.pdf");
    }

    @Test
    void submitAssignmentKeepsLegacyQueryParameterContract() throws Exception {
        UUID assignmentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();

        when(assignmentSubmissionService.submitAssignment(eq(enrollmentUuid), eq(assignmentUuid), eq("Legacy content"), isNull()))
                .thenReturn(submission(UUID.randomUUID(), enrollmentUuid, assignmentUuid));

        mockMvc.perform(post("/api/v1/assignments/{assignmentUuid}/submit", assignmentUuid)
                        .param("enrollmentUuid", enrollmentUuid.toString())
                        .param("content", "Legacy content"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enrollment_uuid").value(enrollmentUuid.toString()));

        verify(assignmentSubmissionService).submitAssignment(enrollmentUuid, assignmentUuid, "Legacy content", null);
    }

    @Test
    void submitAssignmentMultipartStoresFilesAsSubmissionAttachments() throws Exception {
        UUID assignmentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID submissionUuid = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "work.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF".getBytes()
        );

        when(assignmentService.getAssignmentByUuid(assignmentUuid))
                .thenReturn(assignment(assignmentUuid, new String[]{"DOCUMENT"}));
        when(assignmentSubmissionService.submitAssignment(eq(assignmentUuid), any(AssignmentSubmissionRequest.class), eq(true)))
                .thenReturn(submission(submissionUuid, enrollmentUuid, assignmentUuid));
        when(storageService.store(any(MultipartFile.class), eq("assignments/" + assignmentUuid + "/submissions/" + submissionUuid)))
                .thenReturn("assignments/" + assignmentUuid + "/submissions/" + submissionUuid + "/work.pdf");
        when(storageService.getContentType("assignments/" + assignmentUuid + "/submissions/" + submissionUuid + "/work.pdf"))
                .thenReturn(MediaType.APPLICATION_PDF_VALUE);
        when(assignmentSubmissionAttachmentService.createAttachment(any(AssignmentSubmissionAttachmentDTO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(multipart("/api/v1/assignments/{assignmentUuid}/submit", assignmentUuid)
                        .file(file)
                        .param("enrollment_uuid", enrollmentUuid.toString())
                        .param("submission_text", "See attached work"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uuid").value(submissionUuid.toString()));

        verify(assignmentMediaValidationService).validateSubmissionAttachment(
                eq(new String[]{"DOCUMENT"}),
                any(MultipartFile.class)
        );

        ArgumentCaptor<AssignmentSubmissionRequest> requestCaptor =
                ArgumentCaptor.forClass(AssignmentSubmissionRequest.class);
        verify(assignmentSubmissionService).submitAssignment(eq(assignmentUuid), requestCaptor.capture(), eq(true));
        assertThat(requestCaptor.getValue().enrollmentUuid()).isEqualTo(enrollmentUuid);
        assertThat(requestCaptor.getValue().submissionText()).isEqualTo("See attached work");

        ArgumentCaptor<AssignmentSubmissionAttachmentDTO> attachmentCaptor =
                ArgumentCaptor.forClass(AssignmentSubmissionAttachmentDTO.class);
        verify(assignmentSubmissionAttachmentService).createAttachment(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().submissionUuid()).isEqualTo(submissionUuid);
        assertThat(attachmentCaptor.getValue().originalFilename()).isEqualTo("work.pdf");
        assertThat(attachmentCaptor.getValue().mimeType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
    }

    private AssignmentDTO assignment(UUID assignmentUuid, String[] submissionTypes) {
        return new AssignmentDTO(
                assignmentUuid,
                UUID.randomUUID(),
                null,
                null,
                null,
                "Assignment",
                null,
                null,
                null,
                BigDecimal.TEN,
                null,
                submissionTypes,
                true,
                null,
                null,
                null,
                null
        );
    }

    private AssignmentSubmissionDTO submission(UUID submissionUuid, UUID enrollmentUuid, UUID assignmentUuid) {
        return new AssignmentSubmissionDTO(
                submissionUuid,
                enrollmentUuid,
                assignmentUuid,
                "My submission",
                null,
                null,
                SubmissionStatus.SUBMITTED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
