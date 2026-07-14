package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionCreateRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionUpdateRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassRatingSummaryDTO;
import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassReviewDTO;
import apps.sarafrika.elimika.classes.dto.ClassReviewRequest;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateScheduleResponseDTO;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.service.ClassReviewService;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.shared.config.GlobalExceptionHandler;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ClassDefinitionController.class, properties = "app.keycloak.realm=test-realm")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Import({ClassDefinitionControllerTest.MockConfig.class, GlobalExceptionHandler.class})
class ClassDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClassDefinitionServiceInterface classDefinitionService;

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private RequestAuditService requestAuditService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ClassReviewService classReviewService;

    @BeforeEach
    void setUp() {
        reset(classDefinitionService, timetableService, userManagementService, requestAuditService, storageService,
                classReviewService);
    }

    @Test
    void createClassDefinitionForProgramCarriesNewClassMetadataFields() throws Exception {
        UUID programUuid = UUID.randomUUID();
        ClassDefinitionDTO request = sampleRequest(null, null, 30, "#1F6FEB");

        ClassDefinitionDTO responseDto = sampleRequest(
                null,
                programUuid,
                request.classReminderMinutes(),
                request.classColor()
        );

        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(responseDto));

        mockMvc.perform(post("/api/v1/classes/program/{programUuid}", programUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.class_definition.program_uuid").value(programUuid.toString()))
                .andExpect(jsonPath("$.data.class_definition.academic_period_start_date").value("2026-05-01"))
                .andExpect(jsonPath("$.data.class_definition.academic_period_end_date").value("2026-07-31"))
                .andExpect(jsonPath("$.data.class_definition.registration_period_start_date").value("2026-04-15"))
                .andExpect(jsonPath("$.data.class_definition.registration_period_end_date").value("2026-04-15"))
                .andExpect(jsonPath("$.data.class_definition.class_reminder_minutes").value(30))
                .andExpect(jsonPath("$.data.class_definition.class_color").value("#1F6FEB"));

        ArgumentCaptor<ClassDefinitionDTO> captor = ArgumentCaptor.forClass(ClassDefinitionDTO.class);
        verify(classDefinitionService).createClassDefinition(captor.capture());

        ClassDefinitionDTO forwarded = captor.getValue();
        assertEquals(programUuid, forwarded.programUuid());
        assertNull(forwarded.courseUuid());
        assertEquals(request.academicPeriodStartDate(), forwarded.academicPeriodStartDate());
        assertEquals(request.academicPeriodEndDate(), forwarded.academicPeriodEndDate());
        assertEquals(request.registrationPeriodStartDate(), forwarded.registrationPeriodStartDate());
        assertEquals(request.registrationPeriodEndDate(), forwarded.registrationPeriodEndDate());
        assertEquals(request.classReminderMinutes(), forwarded.classReminderMinutes());
        assertEquals(request.classColor(), forwarded.classColor());
    }

    @Test
    void createClassDefinitionMultipartAcceptsFormFieldsAndMedia() throws Exception {
        ClassDefinitionDTO source = sampleRequest(null, null, 30, "#1F6FEB");
        ClassDefinitionCreateRequestDTO request = sampleCreateRequest(source);
        ClassDefinitionDTO responseDto = new ClassDefinitionDTO(
                source.uuid(),
                source.title(),
                source.description(),
                "/api/v1/classes/media/class_thumbnails/" + source.uuid() + "/image.png",
                "/api/v1/classes/media/class_promotional_videos/" + source.uuid() + "/promo.mp4",
                source.defaultInstructorUuid(),
                source.organisationUuid(),
                source.courseUuid(),
                source.programUuid(),
                source.trainingFee(),
                source.classVisibility(),
                source.sessionFormat(),
                source.defaultStartTime(),
                source.defaultEndTime(),
                source.academicPeriodStartDate(),
                source.academicPeriodEndDate(),
                source.registrationPeriodStartDate(),
                source.registrationPeriodEndDate(),
                source.classReminderMinutes(),
                source.classColor(),
                source.locationType(),
                source.locationName(),
                source.locationLatitude(),
                source.locationLongitude(),
                source.meetingLink(),
                source.maxParticipants(),
                source.allowWaitlist(),
                source.isActive(),
                source.sessionTemplates(),
                source.createdDate(),
                source.updatedDate(),
                source.createdBy(),
                source.updatedBy()
        );
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnail",
                "image.png",
                MediaType.IMAGE_PNG_VALUE,
                "image".getBytes()
        );
        MockMultipartFile promotionalVideo = new MockMultipartFile(
                "promotional_video",
                "promo.mp4",
                "video/mp4",
                "video".getBytes()
        );

        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class), any(), any()))
                .thenReturn(new ClassDefinitionResponseDTO(responseDto));

        mockMvc.perform(multipart("/api/v1/classes")
                        .file(thumbnail)
                        .file(promotionalVideo)
                        .param("title", request.title())
                        .param("description", request.description())
                        .param("default_instructor_uuid", request.defaultInstructorUuid().toString())
                        .param("organisation_uuid", request.organisationUuid().toString())
                        .param("training_fee", request.trainingFee().toPlainString())
                        .param("class_visibility", request.classVisibility().name())
                        .param("session_format", request.sessionFormat().name())
                        .param("default_start_time", request.defaultStartTime().toString())
                        .param("default_end_time", request.defaultEndTime().toString())
                        .param("academic_period_start_date", request.academicPeriodStartDate().toString())
                        .param("academic_period_end_date", request.academicPeriodEndDate().toString())
                        .param("registration_period_start_date", request.registrationPeriodStartDate().toString())
                        .param("registration_period_end_date", request.registrationPeriodEndDate().toString())
                        .param("class_reminder_minutes", request.classReminderMinutes().toString())
                        .param("class_color", request.classColor())
                        .param("location_type", request.locationType().name())
                        .param("location_name", request.locationName())
                        .param("location_latitude", request.locationLatitude().toPlainString())
                        .param("location_longitude", request.locationLongitude().toPlainString())
                        .param("meeting_link", request.meetingLink())
                        .param("max_participants", request.maxParticipants().toString())
                        .param("allow_waitlist", request.allowWaitlist().toString())
                        .param("is_active", request.isActive().toString())
                        .param("session_templates", objectMapper.writeValueAsString(request.sessionTemplates())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.class_definition.thumbnail_url").value(responseDto.thumbnailUrl()))
                .andExpect(jsonPath("$.data.class_definition.promotional_video_url").value(responseDto.promotionalVideoUrl()));

        ArgumentCaptor<ClassDefinitionDTO> captor = ArgumentCaptor.forClass(ClassDefinitionDTO.class);
        verify(classDefinitionService).createClassDefinition(captor.capture(), any(), any());
        assertEquals(request.title(), captor.getValue().title());
        assertEquals(request.sessionTemplates().size(), captor.getValue().sessionTemplates().size());
    }

    @Test
    void updateClassDefinitionDoesNotRequireSessionTemplates() throws Exception {
        UUID classUuid = UUID.randomUUID();
        ClassDefinitionDTO existing = sampleRequest(UUID.randomUUID(), null, 30, "#1F6FEB");
        ClassDefinitionUpdateRequestDTO request = sampleUpdateRequest(existing);

        when(classDefinitionService.updateClassDefinition(any(UUID.class), any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(existing));

        mockMvc.perform(put("/api/v1/classes/{uuid}", classUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.class_definition.title").value(existing.title()));

        ArgumentCaptor<ClassDefinitionDTO> captor = ArgumentCaptor.forClass(ClassDefinitionDTO.class);
        verify(classDefinitionService).updateClassDefinition(eq(classUuid), captor.capture());
        assertNull(captor.getValue().sessionTemplates());
    }

    @Test
    void uploadClassThumbnailReturnsUpdatedClassDefinition() throws Exception {
        UUID classUuid = UUID.randomUUID();
        ClassDefinitionDTO responseDto = sampleRequest(UUID.randomUUID(), null, 30, "#1F6FEB");
        responseDto = new ClassDefinitionDTO(
                responseDto.uuid(),
                responseDto.title(),
                responseDto.description(),
                "/api/v1/classes/media/class_thumbnails/" + classUuid + "/image.png",
                responseDto.promotionalVideoUrl(),
                responseDto.defaultInstructorUuid(),
                responseDto.organisationUuid(),
                responseDto.courseUuid(),
                responseDto.programUuid(),
                responseDto.trainingFee(),
                responseDto.classVisibility(),
                responseDto.sessionFormat(),
                responseDto.defaultStartTime(),
                responseDto.defaultEndTime(),
                responseDto.academicPeriodStartDate(),
                responseDto.academicPeriodEndDate(),
                responseDto.registrationPeriodStartDate(),
                responseDto.registrationPeriodEndDate(),
                responseDto.classReminderMinutes(),
                responseDto.classColor(),
                responseDto.locationType(),
                responseDto.locationName(),
                responseDto.locationLatitude(),
                responseDto.locationLongitude(),
                responseDto.meetingLink(),
                responseDto.maxParticipants(),
                responseDto.allowWaitlist(),
                responseDto.isActive(),
                responseDto.sessionTemplates(),
                responseDto.createdDate(),
                responseDto.updatedDate(),
                responseDto.createdBy(),
                responseDto.updatedBy()
        );
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnail",
                "image.png",
                MediaType.IMAGE_PNG_VALUE,
                "image".getBytes()
        );

        when(classDefinitionService.uploadThumbnail(eq(classUuid), any()))
                .thenReturn(new ClassDefinitionResponseDTO(responseDto));

        mockMvc.perform(multipart("/api/v1/classes/{uuid}/thumbnail", classUuid)
                        .file(thumbnail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.class_definition.thumbnail_url").value(responseDto.thumbnailUrl()));
    }

    @Test
    void uploadClassPromotionalVideoReturnsUpdatedClassDefinition() throws Exception {
        UUID classUuid = UUID.randomUUID();
        ClassDefinitionDTO responseDto = sampleRequest(UUID.randomUUID(), null, 30, "#1F6FEB");
        responseDto = new ClassDefinitionDTO(
                responseDto.uuid(),
                responseDto.title(),
                responseDto.description(),
                responseDto.thumbnailUrl(),
                "/api/v1/classes/media/class_promotional_videos/" + classUuid + "/promo.mp4",
                responseDto.defaultInstructorUuid(),
                responseDto.organisationUuid(),
                responseDto.courseUuid(),
                responseDto.programUuid(),
                responseDto.trainingFee(),
                responseDto.classVisibility(),
                responseDto.sessionFormat(),
                responseDto.defaultStartTime(),
                responseDto.defaultEndTime(),
                responseDto.academicPeriodStartDate(),
                responseDto.academicPeriodEndDate(),
                responseDto.registrationPeriodStartDate(),
                responseDto.registrationPeriodEndDate(),
                responseDto.classReminderMinutes(),
                responseDto.classColor(),
                responseDto.locationType(),
                responseDto.locationName(),
                responseDto.locationLatitude(),
                responseDto.locationLongitude(),
                responseDto.meetingLink(),
                responseDto.maxParticipants(),
                responseDto.allowWaitlist(),
                responseDto.isActive(),
                responseDto.sessionTemplates(),
                responseDto.createdDate(),
                responseDto.updatedDate(),
                responseDto.createdBy(),
                responseDto.updatedBy()
        );
        MockMultipartFile promotionalVideo = new MockMultipartFile(
                "promotional_video",
                "promo.mp4",
                "video/mp4",
                "video".getBytes()
        );

        when(classDefinitionService.uploadPromotionalVideo(eq(classUuid), any()))
                .thenReturn(new ClassDefinitionResponseDTO(responseDto));

        mockMvc.perform(multipart("/api/v1/classes/{uuid}/promotional-video", classUuid)
                        .file(promotionalVideo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.class_definition.promotional_video_url").value(responseDto.promotionalVideoUrl()));
    }

    @Test
    void getClassMediaStreamsStoredMedia() throws Exception {
        String storedPath = "class_thumbnails/class-uuid/image.png";
        when(storageService.load(storedPath)).thenReturn(new ByteArrayResource("image".getBytes()));
        when(storageService.getContentType(storedPath)).thenReturn(MediaType.IMAGE_PNG_VALUE);

        mockMvc.perform(get("/api/v1/classes/media/" + storedPath))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"image.png\""));
    }

    @Test
    void addSessionTemplateAppliesTemplateToExistingClass() throws Exception {
        UUID classUuid = UUID.randomUUID();
        ClassSessionTemplateDTO request = new ClassSessionTemplateDTO(
                LocalDateTime.of(2026, 5, 12, 9, 0),
                LocalDateTime.of(2026, 5, 12, 11, 0),
                null,
                ConflictResolutionStrategy.FAIL
        );
        ClassSessionTemplateDTO persistedTemplate = new ClassSessionTemplateDTO(
                UUID.randomUUID(),
                request.startTime(),
                request.endTime(),
                request.recurrence(),
                request.conflictResolution()
        );

        when(classDefinitionService.addSessionTemplate(eq(classUuid), any(ClassSessionTemplateDTO.class)))
                .thenReturn(new ClassSessionTemplateScheduleResponseDTO(
                        classUuid,
                        persistedTemplate,
                        List.of(),
                        List.of()
                ));

        mockMvc.perform(post("/api/v1/classes/{uuid}/session-templates", classUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.class_definition_uuid").value(classUuid.toString()))
                .andExpect(jsonPath("$.data.session_template.uuid").value(persistedTemplate.uuid().toString()));

        verify(classDefinitionService).addSessionTemplate(eq(classUuid), any(ClassSessionTemplateDTO.class));
    }

    @Test
    void submitClassReviewUsesPathClassUuidAndReturnsCreated() throws Exception {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        ClassReviewRequest request = reviewRequest(studentUuid);
        ClassReviewDTO response = review(classDefinitionUuid, studentUuid, 5);

        when(classReviewService.saveClassReview(eq(classDefinitionUuid), any(ClassReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/classes/{uuid}/reviews", classDefinitionUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.class_definition_uuid").value(classDefinitionUuid.toString()))
                .andExpect(jsonPath("$.data.student_uuid").value(studentUuid.toString()))
                .andExpect(jsonPath("$.data.rating").value(5));

        ArgumentCaptor<ClassReviewRequest> captor = ArgumentCaptor.forClass(ClassReviewRequest.class);
        verify(classReviewService).saveClassReview(eq(classDefinitionUuid), captor.capture());
        assertEquals(studentUuid, captor.getValue().studentUuid());
    }

    @Test
    void getClassReviewsReturnsPagedResponse() throws Exception {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);

        when(classReviewService.getReviewsForClass(eq(classDefinitionUuid), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review(classDefinitionUuid, studentUuid, 4)), pageable, 1));

        mockMvc.perform(get("/api/v1/classes/{uuid}/reviews?page=0&size=20", classDefinitionUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].class_definition_uuid").value(classDefinitionUuid.toString()))
                .andExpect(jsonPath("$.data.content[0].rating").value(4))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(1));
    }

    @Test
    void getClassRatingSummaryReturnsAverageAndCount() throws Exception {
        UUID classDefinitionUuid = UUID.randomUUID();

        when(classReviewService.getRatingSummary(classDefinitionUuid))
                .thenReturn(new ClassRatingSummaryDTO(classDefinitionUuid, 4.5, 2L));

        mockMvc.perform(get("/api/v1/classes/{uuid}/reviews/summary", classDefinitionUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.class_definition_uuid").value(classDefinitionUuid.toString()))
                .andExpect(jsonPath("$.data.average_rating").value(4.5))
                .andExpect(jsonPath("$.data.review_count").value(2));
    }

    @Test
    void createClassDefinitionRejectsInvalidAcademicPeriod() throws Exception {
        ClassDefinitionDTO request = sampleRequest(
                UUID.randomUUID(),
                null,
                30,
                "#1F6FEB",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 15)
        );

        mockMvc.perform(post("/api/v1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(classDefinitionService);
    }

    @Test
    void createClassDefinitionRejectsInvalidRegistrationPeriod() throws Exception {
        ClassDefinitionDTO request = sampleRequest(
                UUID.randomUUID(),
                null,
                30,
                "#1F6FEB",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 15)
        );

        mockMvc.perform(post("/api/v1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(classDefinitionService);
    }

    @Test
    void createClassDefinitionRejectsInvalidReminderMinutesAndColor() throws Exception {
        ClassDefinitionDTO request = sampleRequest(
                UUID.randomUUID(),
                null,
                -5,
                "blue"
        );

        mockMvc.perform(post("/api/v1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(classDefinitionService);
    }

    @Test
    void createClassDefinitionRejectsBlankLocationTypeAsBadRequest() throws Exception {
        ClassDefinitionDTO request = sampleRequest(
                UUID.randomUUID(),
                null,
                30,
                "#1F6FEB"
        );
        String payload = objectMapper.writeValueAsString(request)
                .replace("\"location_type\":\"HYBRID\"", "\"location_type\":\"\"");

        mockMvc.perform(post("/api/v1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.error.location_type")
                        .value("location_type must be one of ONLINE, IN_PERSON, HYBRID"));

        verifyNoInteractions(classDefinitionService);
    }

    private ClassDefinitionDTO sampleRequest(UUID courseUuid,
                                             UUID programUuid,
                                             Integer classReminderMinutes,
                                             String classColor) {
        return sampleRequest(
                courseUuid,
                programUuid,
                classReminderMinutes,
                classColor,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 15)
        );
    }

    private ClassDefinitionDTO sampleRequest(UUID courseUuid,
                                             UUID programUuid,
                                             Integer classReminderMinutes,
                                             String classColor,
                                             LocalDate academicPeriodStartDate,
                                             LocalDate academicPeriodEndDate,
                                             LocalDate registrationPeriodStartDate,
                                             LocalDate registrationPeriodEndDate) {
        return new ClassDefinitionDTO(
                UUID.randomUUID(),
                "Data Science Cohort",
                "Applied analytics class",
                UUID.randomUUID(),
                UUID.randomUUID(),
                courseUuid,
                programUuid,
                new BigDecimal("240.00"),
                ClassVisibility.PUBLIC,
                SessionFormat.GROUP,
                LocalDateTime.of(2026, 5, 5, 9, 0),
                LocalDateTime.of(2026, 5, 5, 11, 0),
                academicPeriodStartDate,
                academicPeriodEndDate,
                registrationPeriodStartDate,
                registrationPeriodEndDate,
                classReminderMinutes,
                classColor,
                LocationType.HYBRID,
                "Nairobi HQ - Room 4",
                new BigDecimal("-1.292066"),
                new BigDecimal("36.821945"),
                "https://meet.google.com/abc-defg-hij",
                25,
                true,
                true,
                List.of(new ClassSessionTemplateDTO(
                        LocalDateTime.of(2026, 5, 5, 9, 0),
                        LocalDateTime.of(2026, 5, 5, 11, 0),
                        new ClassRecurrenceDTO(
                                ClassRecurrenceDTO.RecurrenceType.WEEKLY,
                                1,
                                "MONDAY,WEDNESDAY",
                                null,
                                LocalDate.of(2026, 7, 31),
                                8
                        ),
                        ConflictResolutionStrategy.FAIL
                )),
                null,
                null,
                null,
                null
        );
    }

    private ClassDefinitionCreateRequestDTO sampleCreateRequest(ClassDefinitionDTO source) {
        return new ClassDefinitionCreateRequestDTO(
                source.title(),
                source.description(),
                source.thumbnailUrl(),
                source.promotionalVideoUrl(),
                source.defaultInstructorUuid(),
                source.organisationUuid(),
                source.courseUuid(),
                source.programUuid(),
                source.trainingFee(),
                source.classVisibility(),
                source.sessionFormat(),
                source.defaultStartTime(),
                source.defaultEndTime(),
                source.academicPeriodStartDate(),
                source.academicPeriodEndDate(),
                source.registrationPeriodStartDate(),
                source.registrationPeriodEndDate(),
                source.classReminderMinutes(),
                source.classColor(),
                source.locationType(),
                source.locationName(),
                source.locationLatitude(),
                source.locationLongitude(),
                source.meetingLink(),
                source.maxParticipants(),
                source.allowWaitlist(),
                source.isActive(),
                source.sessionTemplates()
        );
    }

    private ClassDefinitionUpdateRequestDTO sampleUpdateRequest(ClassDefinitionDTO source) {
        return new ClassDefinitionUpdateRequestDTO(
                source.title(),
                source.description(),
                source.thumbnailUrl(),
                source.promotionalVideoUrl(),
                source.defaultInstructorUuid(),
                source.organisationUuid(),
                source.courseUuid(),
                source.programUuid(),
                source.trainingFee(),
                source.classVisibility(),
                source.sessionFormat(),
                source.defaultStartTime(),
                source.defaultEndTime(),
                source.academicPeriodStartDate(),
                source.academicPeriodEndDate(),
                source.registrationPeriodStartDate(),
                source.registrationPeriodEndDate(),
                source.classReminderMinutes(),
                source.classColor(),
                source.locationType(),
                source.locationName(),
                source.locationLatitude(),
                source.locationLongitude(),
                source.meetingLink(),
                source.maxParticipants(),
                source.allowWaitlist(),
                source.isActive()
        );
    }

    private ClassReviewRequest reviewRequest(UUID studentUuid) {
        return new ClassReviewRequest(
                studentUuid,
                5,
                "Practical class",
                "The class session was clear and hands-on.",
                false
        );
    }

    private ClassReviewDTO review(UUID classDefinitionUuid, UUID studentUuid, int rating) {
        return new ClassReviewDTO(
                UUID.randomUUID(),
                classDefinitionUuid,
                studentUuid,
                rating,
                "Practical class",
                "The class session was clear and hands-on.",
                false,
                null,
                null,
                null,
                null
        );
    }

    static class MockConfig {
        @Bean
        ClassDefinitionServiceInterface classDefinitionService() {
            return Mockito.mock(ClassDefinitionServiceInterface.class);
        }

        @Bean
        TimetableService timetableService() {
            return Mockito.mock(TimetableService.class);
        }

        @Bean
        UserManagementService userManagementService() {
            return Mockito.mock(UserManagementService.class);
        }

        @Bean
        RequestAuditService requestAuditService() {
            return Mockito.mock(RequestAuditService.class);
        }

        @Bean
        StorageService storageService() {
            return Mockito.mock(StorageService.class);
        }

        @Bean
        apps.sarafrika.elimika.shared.storage.service.MediaServeService mediaServeService(StorageService storageService) {
            return new apps.sarafrika.elimika.shared.storage.service.MediaServeService(storageService);
        }

        @Bean
        ClassReviewService classReviewService() {
            return Mockito.mock(ClassReviewService.class);
        }

        @Bean
        StorageProperties storageProperties() {
            StorageProperties storageProperties = new StorageProperties();
            StorageProperties.Folders folders = new StorageProperties.Folders();
            folders.setClassThumbnails("class_thumbnails");
            folders.setClassPromotionalVideos("class_promotional_videos");
            storageProperties.setFolders(folders);
            return storageProperties;
        }
    }
    @Test
    void invalidUuidPathSegmentReturnsBadRequestNotServerError() throws Exception {
        mockMvc.perform(get("/api/v1/classes/instructor/{instructorUuid}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"));
    }

    @Test
    void unknownPathReturnsNotFoundNotServerError() throws Exception {
        mockMvc.perform(get("/api/v1/classes/{uuid}/no-such-resource/extra", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    void getInstructorPayablesForOrganisationReturnsPayables() throws Exception {
        UUID organisationUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        when(classDefinitionService.getInstructorPayablesForOrganisation(organisationUuid))
                .thenReturn(List.of(new apps.sarafrika.elimika.classes.dto.OrganisationInstructorPayableDTO(
                        instructorUuid, new BigDecimal("480.00"), 2L, 6L)));

        mockMvc.perform(get("/api/v1/classes/organisation/{organisationUuid}/instructor-payables", organisationUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].instructor_uuid").value(instructorUuid.toString()))
                .andExpect(jsonPath("$.data[0].amount_owed").value(480.00))
                .andExpect(jsonPath("$.data[0].class_count").value(2))
                .andExpect(jsonPath("$.data[0].session_count").value(6));

        verify(classDefinitionService).getInstructorPayablesForOrganisation(organisationUuid);
    }

}
