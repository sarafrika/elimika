package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.internal.ClassMediaValidationService;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassSchedulingConflictRepository;
import apps.sarafrika.elimika.classes.repository.ClassSessionTemplateRepository;
import apps.sarafrika.elimika.classes.util.enums.ClassRecurrenceType;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.spi.ClassScheduleService;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassDefinitionServiceImplTest {

    @Mock
    private ClassDefinitionRepository classDefinitionRepository;

    @Mock
    private ClassSchedulingConflictRepository classSchedulingConflictRepository;

    @Mock
    private ClassSessionTemplateRepository classSessionTemplateRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CourseInfoService courseInfoService;

    @Mock
    private CourseTrainingApprovalSpi courseTrainingApprovalSpi;

    @Mock
    private ObjectProvider<TimetableService> timetableServiceProvider;

    @Mock
    private ObjectProvider<ClassScheduleService> classScheduleServiceProvider;

    @Mock
    private TimetableService timetableService;

    @Mock
    private StorageService storageService;

    @Mock
    private ClassMediaValidationService classMediaValidationService;

    private StorageProperties storageProperties;

    private ClassDefinitionServiceImpl service;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        StorageProperties.Folders folders = new StorageProperties.Folders();
        folders.setClassThumbnails("class_thumbnails");
        folders.setClassPromotionalVideos("class_promotional_videos");
        storageProperties.setFolders(folders);

        service = new ClassDefinitionServiceImpl(
                classDefinitionRepository,
                classSchedulingConflictRepository,
                classSessionTemplateRepository,
                availabilityService,
                eventPublisher,
                courseInfoService,
                courseTrainingApprovalSpi,
                timetableServiceProvider,
                classScheduleServiceProvider,
                storageService,
                storageProperties,
                classMediaValidationService
        );
    }

    @Test
    void createClassDefinitionPersistsSessionTemplatesAndReturnsThem() {
        UUID classUuid = UUID.randomUUID();
        ClassDefinitionDTO request = sampleClassDefinition();

        when(classDefinitionRepository.save(any(ClassDefinition.class))).thenAnswer(invocation -> {
            ClassDefinition entity = invocation.getArgument(0);
            entity.setUuid(classUuid);
            return entity;
        });
        when(classSessionTemplateRepository.saveAll(any())).thenAnswer(invocation -> {
            List<ClassSessionTemplate> templates = invocation.getArgument(0);
            templates.forEach(template -> template.setUuid(UUID.randomUUID()));
            return templates;
        });
        when(availabilityService.getAvailabilityForInstructor(request.defaultInstructorUuid())).thenReturn(List.of());
        when(availabilityService.isInstructorAvailable(
                request.defaultInstructorUuid(),
                request.sessionTemplates().getFirst().startTime(),
                request.sessionTemplates().getFirst().endTime()))
                .thenReturn(true);
        when(timetableServiceProvider.getIfAvailable()).thenReturn(timetableService);
        when(classScheduleServiceProvider.getIfAvailable()).thenReturn(null);
        when(timetableService.hasInstructorConflict(any(UUID.class), any(ScheduleRequestDTO.class))).thenReturn(false);
        when(timetableService.scheduleClass(any(ScheduleRequestDTO.class))).thenReturn(sampleScheduledInstance(classUuid));

        ClassDefinitionResponseDTO response = service.createClassDefinition(request);

        assertThat(response.classDefinition().sessionTemplates()).hasSize(1);
        assertThat(response.classDefinition().sessionTemplates().getFirst().uuid()).isNotNull();

        verify(classSessionTemplateRepository).saveAll(argThat(templates -> {
            List<ClassSessionTemplate> captured = StreamSupport.stream(templates.spliterator(), false).toList();
            assertThat(captured)
                    .hasSize(1)
                    .first()
                    .satisfies(template -> {
                        assertThat(template.getClassDefinitionUuid()).isEqualTo(classUuid);
                        assertThat(template.getTemplateOrder()).isZero();
                        assertThat(template.getConflictResolution()).isEqualTo(ConflictResolutionStrategy.FAIL);
                    });
            return true;
        }));
        verify(timetableService).scheduleClass(any(ScheduleRequestDTO.class));
    }

    @Test
    void createClassDefinitionWithMediaStoresFilesUnderCreatedClassFolder() {
        UUID classUuid = UUID.randomUUID();
        ClassDefinitionDTO request = sampleClassDefinition();
        MockMultipartFile thumbnail = new MockMultipartFile("thumbnail", "image.png", "image/png", "image".getBytes());
        MockMultipartFile promotionalVideo = new MockMultipartFile("promotional_video", "promo.mp4", "video/mp4", "video".getBytes());
        AtomicReference<ClassDefinition> savedEntity = new AtomicReference<>();

        when(classDefinitionRepository.save(any(ClassDefinition.class))).thenAnswer(invocation -> {
            ClassDefinition entity = invocation.getArgument(0);
            if (entity.getUuid() == null) {
                entity.setUuid(classUuid);
            }
            savedEntity.set(entity);
            return entity;
        });
        when(classDefinitionRepository.findByUuid(classUuid)).thenAnswer(invocation -> Optional.of(savedEntity.get()));
        when(classSessionTemplateRepository.saveAll(any())).thenAnswer(invocation -> {
            List<ClassSessionTemplate> templates = invocation.getArgument(0);
            templates.forEach(template -> template.setUuid(UUID.randomUUID()));
            return templates;
        });
        when(classSessionTemplateRepository.findByClassDefinitionUuidOrderByTemplateOrderAscCreatedDateAsc(classUuid))
                .thenReturn(List.of());
        when(availabilityService.getAvailabilityForInstructor(request.defaultInstructorUuid())).thenReturn(List.of());
        when(availabilityService.isInstructorAvailable(
                request.defaultInstructorUuid(),
                request.sessionTemplates().getFirst().startTime(),
                request.sessionTemplates().getFirst().endTime()))
                .thenReturn(true);
        when(timetableServiceProvider.getIfAvailable()).thenReturn(timetableService);
        when(classScheduleServiceProvider.getIfAvailable()).thenReturn(null);
        when(timetableService.hasInstructorConflict(any(UUID.class), any(ScheduleRequestDTO.class))).thenReturn(false);
        when(timetableService.scheduleClass(any(ScheduleRequestDTO.class))).thenReturn(sampleScheduledInstance(classUuid));
        when(storageService.store(thumbnail, "class_thumbnails/" + classUuid))
                .thenReturn("class_thumbnails/" + classUuid + "/generated.png");
        when(storageService.store(promotionalVideo, "class_promotional_videos/" + classUuid))
                .thenReturn("class_promotional_videos/" + classUuid + "/generated.mp4");

        ClassDefinitionResponseDTO response = service.createClassDefinition(request, thumbnail, promotionalVideo);

        assertThat(response.classDefinition().thumbnailUrl())
                .isEqualTo("/api/v1/classes/media/class_thumbnails/" + classUuid + "/generated.png");
        assertThat(response.classDefinition().promotionalVideoUrl())
                .isEqualTo("/api/v1/classes/media/class_promotional_videos/" + classUuid + "/generated.mp4");
        verify(classMediaValidationService).validateThumbnail(thumbnail);
        verify(classMediaValidationService).validatePromotionalVideo(promotionalVideo);
        verify(storageService).store(thumbnail, "class_thumbnails/" + classUuid);
        verify(storageService).store(promotionalVideo, "class_promotional_videos/" + classUuid);
    }

    @Test
    void getClassDefinitionReturnsPersistedSessionTemplates() {
        UUID classUuid = UUID.randomUUID();
        ClassDefinition entity = sampleClassDefinitionEntity();
        entity.setUuid(classUuid);
        ClassSessionTemplate template = sampleSessionTemplate(classUuid);
        template.setUuid(UUID.randomUUID());

        when(classDefinitionRepository.findByUuid(classUuid)).thenReturn(Optional.of(entity));
        when(classSessionTemplateRepository.findByClassDefinitionUuidOrderByTemplateOrderAscCreatedDateAsc(classUuid))
                .thenReturn(List.of(template));
        when(classScheduleServiceProvider.getIfAvailable()).thenReturn(null);

        ClassDefinitionResponseDTO response = service.getClassDefinition(classUuid);

        assertThat(response.classDefinition().sessionTemplates()).hasSize(1);
        ClassSessionTemplateDTO resultTemplate = response.classDefinition().sessionTemplates().getFirst();
        assertThat(resultTemplate.uuid()).isEqualTo(template.getUuid());
        assertThat(resultTemplate.recurrence().recurrenceType()).isEqualTo(ClassRecurrenceDTO.RecurrenceType.WEEKLY);
    }

    @Test
    void uploadThumbnailStoresInClassFolderAndPersistsMediaUrl() {
        UUID classUuid = UUID.randomUUID();
        ClassDefinition entity = sampleClassDefinitionEntity();
        entity.setUuid(classUuid);
        MockMultipartFile file = new MockMultipartFile("thumbnail", "image.png", "image/png", "image".getBytes());
        String storedPath = "class_thumbnails/" + classUuid + "/generated.png";

        when(classDefinitionRepository.findByUuid(classUuid)).thenReturn(Optional.of(entity));
        when(storageService.store(file, "class_thumbnails/" + classUuid)).thenReturn(storedPath);
        when(classDefinitionRepository.save(any(ClassDefinition.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classSessionTemplateRepository.findByClassDefinitionUuidOrderByTemplateOrderAscCreatedDateAsc(classUuid))
                .thenReturn(List.of());

        ClassDefinitionResponseDTO response = service.uploadThumbnail(classUuid, file);

        assertThat(response.classDefinition().thumbnailUrl())
                .isEqualTo("/api/v1/classes/media/class_thumbnails/" + classUuid + "/generated.png");
        assertThat(entity.getThumbnailUrl()).isEqualTo(response.classDefinition().thumbnailUrl());
        verify(classMediaValidationService).validateThumbnail(file);
        verify(storageService).store(file, "class_thumbnails/" + classUuid);
    }

    @Test
    void uploadPromotionalVideoStoresInClassFolderAndPersistsMediaUrl() {
        UUID classUuid = UUID.randomUUID();
        ClassDefinition entity = sampleClassDefinitionEntity();
        entity.setUuid(classUuid);
        MockMultipartFile file = new MockMultipartFile("promotional_video", "promo.mp4", "video/mp4", "video".getBytes());
        String storedPath = "class_promotional_videos/" + classUuid + "/generated.mp4";

        when(classDefinitionRepository.findByUuid(classUuid)).thenReturn(Optional.of(entity));
        when(storageService.store(file, "class_promotional_videos/" + classUuid)).thenReturn(storedPath);
        when(classDefinitionRepository.save(any(ClassDefinition.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classSessionTemplateRepository.findByClassDefinitionUuidOrderByTemplateOrderAscCreatedDateAsc(classUuid))
                .thenReturn(List.of());

        ClassDefinitionResponseDTO response = service.uploadPromotionalVideo(classUuid, file);

        assertThat(response.classDefinition().promotionalVideoUrl())
                .isEqualTo("/api/v1/classes/media/class_promotional_videos/" + classUuid + "/generated.mp4");
        assertThat(entity.getPromotionalVideoUrl()).isEqualTo(response.classDefinition().promotionalVideoUrl());
        verify(classMediaValidationService).validatePromotionalVideo(file);
        verify(storageService).store(file, "class_promotional_videos/" + classUuid);
    }

    private ClassDefinitionDTO sampleClassDefinition() {
        return new ClassDefinitionDTO(
                null,
                "Data Science Cohort",
                "Applied analytics class",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                ClassVisibility.PUBLIC,
                SessionFormat.GROUP,
                LocalDateTime.of(2026, 6, 12, 9, 0),
                LocalDateTime.of(2026, 6, 12, 11, 0),
                null,
                null,
                null,
                null,
                30,
                "#1F6FEB",
                LocationType.ONLINE,
                null,
                null,
                null,
                "https://meet.google.com/abc-defg-hij",
                25,
                true,
                true,
                List.of(new ClassSessionTemplateDTO(
                        LocalDateTime.of(2026, 6, 12, 9, 0),
                        LocalDateTime.of(2026, 6, 12, 11, 0),
                        new ClassRecurrenceDTO(
                                ClassRecurrenceDTO.RecurrenceType.WEEKLY,
                                1,
                                "FRIDAY",
                                null,
                                null,
                                1
                        ),
                        ConflictResolutionStrategy.FAIL
                )),
                null,
                null,
                null,
                null
        );
    }

    private ClassDefinition sampleClassDefinitionEntity() {
        ClassDefinition entity = new ClassDefinition();
        entity.setTitle("Data Science Cohort");
        entity.setDescription("Applied analytics class");
        entity.setDefaultInstructorUuid(UUID.randomUUID());
        entity.setOrganisationUuid(UUID.randomUUID());
        entity.setClassVisibility(ClassVisibility.PUBLIC);
        entity.setSessionFormat(SessionFormat.GROUP);
        entity.setDefaultStartTime(LocalDateTime.of(2026, 6, 12, 9, 0));
        entity.setDefaultEndTime(LocalDateTime.of(2026, 6, 12, 11, 0));
        entity.setClassReminderMinutes(30);
        entity.setClassColor("#1F6FEB");
        entity.setLocationType(LocationType.ONLINE);
        entity.setMeetingLink("https://meet.google.com/abc-defg-hij");
        entity.setMaxParticipants(25);
        entity.setAllowWaitlist(true);
        entity.setIsActive(true);
        return entity;
    }

    private ClassSessionTemplate sampleSessionTemplate(UUID classUuid) {
        ClassSessionTemplate template = new ClassSessionTemplate();
        template.setClassDefinitionUuid(classUuid);
        template.setTemplateOrder(0);
        template.setStartTime(LocalDateTime.of(2026, 6, 12, 9, 0));
        template.setEndTime(LocalDateTime.of(2026, 6, 12, 11, 0));
        template.setRecurrenceType(ClassRecurrenceType.WEEKLY);
        template.setIntervalValue(1);
        template.setDaysOfWeek("FRIDAY");
        template.setOccurrenceCount(1);
        template.setConflictResolution(ConflictResolutionStrategy.FAIL);
        return template;
    }

    private ScheduledInstanceDTO sampleScheduledInstance(UUID classUuid) {
        return new ScheduledInstanceDTO(
                UUID.randomUUID(),
                classUuid,
                UUID.randomUUID(),
                LocalDateTime.of(2026, 6, 12, 9, 0),
                LocalDateTime.of(2026, 6, 12, 11, 0),
                "UTC",
                "Data Science Cohort",
                "ONLINE",
                null,
                null,
                null,
                25,
                SchedulingStatus.SCHEDULED,
                null,
                null,
                null,
                null,
                null
        );
    }
}
