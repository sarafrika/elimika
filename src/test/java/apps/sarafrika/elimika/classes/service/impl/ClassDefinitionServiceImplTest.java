package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.internal.BranchLocationResolver;
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
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import apps.sarafrika.elimika.shared.storage.service.MediaUploadRequest;
import apps.sarafrika.elimika.shared.storage.service.MediaValidationService;
import apps.sarafrika.elimika.shared.storage.service.StoredMedia;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import apps.sarafrika.elimika.tenancy.spi.BranchLocation;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassDefinitionServiceImplTest {

    @Mock
    private ClassDefinitionRepository classDefinitionRepository;

    @Mock
    private apps.sarafrika.elimika.shared.security.DomainSecurityService classReadDomainSecurityService;

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
    private apps.sarafrika.elimika.shared.spi.payout.InstructorPayableLookupService instructorPayableLookupService;

    @Mock
    private TimetableService timetableService;

    @Mock
    private MediaStorageService mediaStorageService;

    @Mock
    private MediaValidationService mediaValidationService;

    private StorageProperties storageProperties;

    @org.mockito.Mock
    private apps.sarafrika.elimika.classes.repository.ClassDefinitionResourceRepository classDefinitionResourceRepository;

    @org.mockito.Mock
    private apps.sarafrika.elimika.resourcing.spi.ResourceBookingService resourceBookingService;

    @org.mockito.Mock
    private apps.sarafrika.elimika.resourcing.spi.ResourceLookupService resourceLookupService;

    @Mock
    private TrainingBranchLookupService trainingBranchLookupService;

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
                classReadDomainSecurityService,
                // Built for real: outside a request there is no acting-domain header, so the cap
                // resolves to "unspecified" and permits every domain — the uncapped behaviour these
                // cases exercise, and what a caller that sends no header keeps getting.
                new apps.sarafrika.elimika.shared.security.ActingDomainCap(
                        new apps.sarafrika.elimika.shared.security.ActingDomainResolver(
                                new apps.sarafrika.elimika.shared.security.RequestScopedCache())),
                classSchedulingConflictRepository,
                classSessionTemplateRepository,
                classDefinitionResourceRepository,
                resourceBookingService,
                resourceLookupService,
                availabilityService,
                eventPublisher,
                courseInfoService,
                courseTrainingApprovalSpi,
                timetableServiceProvider,
                classScheduleServiceProvider,
                instructorPayableLookupService,
                mediaStorageService,
                mediaValidationService,
                storageProperties,
                new BranchLocationResolver(trainingBranchLookupService)
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
                        assertThat(template.getTimezone()).isEqualTo("Africa/Nairobi");
                        assertThat(template.getConflictResolution()).isEqualTo(ConflictResolutionStrategy.FAIL);
                    });
            return true;
        }));
        verify(timetableService).scheduleClass(argThat(scheduleRequest ->
                "Africa/Nairobi".equals(scheduleRequest.timezone())));
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
        when(mediaStorageService.store(argThat((MediaUploadRequest r) -> r != null && r.file() == thumbnail)))
                .thenReturn(new StoredMedia("class_thumbnails/" + classUuid + "/generated.png", "image.png", 5, "image/png"));
        when(mediaStorageService.store(argThat((MediaUploadRequest r) -> r != null && r.file() == promotionalVideo)))
                .thenReturn(new StoredMedia("class_promotional_videos/" + classUuid + "/generated.mp4", "promo.mp4", 5, "video/mp4"));

        ClassDefinitionResponseDTO response = service.createClassDefinition(request, thumbnail, promotionalVideo);

        assertThat(response.classDefinition().thumbnailUrl())
                .isEqualTo("/api/v1/files/class_thumbnails/" + classUuid + "/generated.png");
        assertThat(response.classDefinition().promotionalVideoUrl())
                .isEqualTo("/api/v1/files/class_promotional_videos/" + classUuid + "/generated.mp4");
        verify(mediaValidationService).validate(thumbnail, MediaCategory.THUMBNAIL);
        verify(mediaValidationService).validate(promotionalVideo, MediaCategory.VIDEO);
        verify(mediaStorageService).store(argThat((MediaUploadRequest r) ->
                r != null && r.file() == thumbnail && r.folder().equals("class_thumbnails/" + classUuid)));
        verify(mediaStorageService).store(argThat((MediaUploadRequest r) ->
                r != null && r.file() == promotionalVideo && r.folder().equals("class_promotional_videos/" + classUuid)));
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
        when(mediaStorageService.store(any(MediaUploadRequest.class)))
                .thenReturn(new StoredMedia(storedPath, "image.png", 5, "image/png"));
        when(classDefinitionRepository.save(any(ClassDefinition.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classSessionTemplateRepository.findByClassDefinitionUuidOrderByTemplateOrderAscCreatedDateAsc(classUuid))
                .thenReturn(List.of());

        ClassDefinitionResponseDTO response = service.uploadThumbnail(classUuid, file);

        assertThat(response.classDefinition().thumbnailUrl())
                .isEqualTo("/api/v1/files/class_thumbnails/" + classUuid + "/generated.png");
        assertThat(entity.getThumbnailUrl()).isEqualTo(storedPath);
        verify(mediaStorageService).store(argThat((MediaUploadRequest r) ->
                r != null && r.file() == file && r.folder().equals("class_thumbnails/" + classUuid)
                        && r.category() == MediaCategory.THUMBNAIL));
    }

    @Test
    void uploadPromotionalVideoStoresInClassFolderAndPersistsMediaUrl() {
        UUID classUuid = UUID.randomUUID();
        ClassDefinition entity = sampleClassDefinitionEntity();
        entity.setUuid(classUuid);
        MockMultipartFile file = new MockMultipartFile("promotional_video", "promo.mp4", "video/mp4", "video".getBytes());
        String storedPath = "class_promotional_videos/" + classUuid + "/generated.mp4";

        when(classDefinitionRepository.findByUuid(classUuid)).thenReturn(Optional.of(entity));
        when(mediaStorageService.store(any(MediaUploadRequest.class)))
                .thenReturn(new StoredMedia(storedPath, "promo.mp4", 5, "video/mp4"));
        when(classDefinitionRepository.save(any(ClassDefinition.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classSessionTemplateRepository.findByClassDefinitionUuidOrderByTemplateOrderAscCreatedDateAsc(classUuid))
                .thenReturn(List.of());

        ClassDefinitionResponseDTO response = service.uploadPromotionalVideo(classUuid, file);

        assertThat(response.classDefinition().promotionalVideoUrl())
                .isEqualTo("/api/v1/files/class_promotional_videos/" + classUuid + "/generated.mp4");
        assertThat(entity.getPromotionalVideoUrl()).isEqualTo(storedPath);
        verify(mediaStorageService).store(argThat((MediaUploadRequest r) ->
                r != null && r.file() == file && r.folder().equals("class_promotional_videos/" + classUuid)
                        && r.category() == MediaCategory.VIDEO));
    }

    private ClassDefinitionDTO sampleClassDefinition() {
        return sampleClassDefinition(UUID.randomUUID(), null, LocationType.ONLINE, null, null, null);
    }

    private ClassDefinitionDTO sampleClassDefinition(UUID organisationUuid,
                                                     UUID branchUuid,
                                                     LocationType locationType,
                                                     String locationName,
                                                     BigDecimal latitude,
                                                     BigDecimal longitude) {
        return new ClassDefinitionDTO(
                null,
                "Data Science Cohort",
                "Applied analytics class",
                UUID.randomUUID(),
                organisationUuid,
                branchUuid,
                null,
                null,
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
                locationType,
                locationName,
                latitude,
                longitude,
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
                        "Africa/Nairobi",
                        ConflictResolutionStrategy.FAIL
                )),
                null,
                null,
                null,
                null
        );
    }


    // ── Instructor pay is the organisation's cost, not the learner's business ──────────────────

    private ClassDefinition classWithPay() {
        ClassDefinition entity = sampleClassDefinitionEntity();
        entity.setUuid(UUID.randomUUID());
        entity.setSalePrice(new java.math.BigDecimal("3000.00"));
        entity.setInstructorPay(new java.math.BigDecimal("2000.00"));
        when(classDefinitionRepository.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));
        when(classSessionTemplateRepository
                .findByClassDefinitionUuidOrderByTemplateOrderAscCreatedDateAsc(entity.getUuid()))
                .thenReturn(List.of());
        return entity;
    }

    @Test
    @DisplayName("a learner reading a class sees the sale price but not the instructor's pay")
    void instructorPayIsWithheldFromLearners() {
        ClassDefinition entity = classWithPay();
        when(classReadDomainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(classReadDomainSecurityService.isInstructorWithUuid(any())).thenReturn(false);
        when(classReadDomainSecurityService.belongsToOrganisationWithDomain(any(), any()))
                .thenReturn(false);

        ClassDefinitionDTO dto = service.getClassDefinition(entity.getUuid()).classDefinition();

        assertThat(dto.salePrice()).isEqualByComparingTo("3000.00");
        assertThat(dto.instructorPay()).isNull();
    }

    @Test
    @DisplayName("a manager of the owning organisation still sees the instructor's pay")
    void instructorPayIsVisibleToTheOwningOrganisation() {
        ClassDefinition entity = classWithPay();
        when(classReadDomainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(classReadDomainSecurityService.isInstructorWithUuid(any())).thenReturn(false);
        when(classReadDomainSecurityService.belongsToOrganisationWithDomain(
                eq(entity.getOrganisationUuid()), any())).thenReturn(true);

        ClassDefinitionDTO dto = service.getClassDefinition(entity.getUuid()).classDefinition();

        assertThat(dto.instructorPay()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("the assigned instructor sees what they are being paid")
    void instructorPayIsVisibleToTheAssignedInstructor() {
        ClassDefinition entity = classWithPay();
        when(classReadDomainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(classReadDomainSecurityService.isInstructorWithUuid(entity.getDefaultInstructorUuid()))
                .thenReturn(true);

        ClassDefinitionDTO dto = service.getClassDefinition(entity.getUuid()).classDefinition();

        assertThat(dto.instructorPay()).isEqualByComparingTo("2000.00");
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
        template.setTimezone("Africa/Nairobi");
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

    // ── The class's branch decides where it is delivered ──────────────────────────────────────

    private static final UUID BRANCH_UUID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    private static final BigDecimal BRANCH_LATITUDE = new BigDecimal("-1.221800");
    private static final BigDecimal BRANCH_LONGITUDE = new BigDecimal("36.897000");

    @Test
    void createCopiesBranchPinForInPersonClass() {
        UUID organisationUuid = UUID.randomUUID();
        ClassDefinitionDTO request = sampleClassDefinition(organisationUuid, BRANCH_UUID, LocationType.IN_PERSON,
                "Typed by the client", new BigDecimal("10.0"), new BigDecimal("10.0"));
        when(trainingBranchLookupService.findBranch(organisationUuid, BRANCH_UUID)).thenReturn(Optional.of(
                new BranchLocation(BRANCH_UUID, organisationUuid, "Main Campus", "Kasarani, Nairobi",
                        new BigDecimal("-1.2218004"), BRANCH_LONGITUDE, true)));
        AtomicReference<ClassDefinition> saved = stubSuccessfulCreate(request);

        service.createClassDefinition(request);

        assertThat(saved.get().getBranchUuid()).isEqualTo(BRANCH_UUID);
        assertThat(saved.get().getLocationName()).isEqualTo("Main Campus · Kasarani, Nairobi");
        assertThat(saved.get().getLocationLatitude()).isEqualTo(BRANCH_LATITUDE);
        assertThat(saved.get().getLocationLongitude()).isEqualTo(BRANCH_LONGITUDE);
    }

    @Test
    void createRefusesBranchOfAnotherOrganisation() {
        UUID organisationUuid = UUID.randomUUID();
        ClassDefinitionDTO request = sampleClassDefinition(organisationUuid, BRANCH_UUID, LocationType.IN_PERSON,
                null, null, null);
        when(trainingBranchLookupService.findBranch(organisationUuid, BRANCH_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createClassDefinition(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training branch %s does not belong to organisation %s", BRANCH_UUID, organisationUuid);
        verify(classDefinitionRepository, never()).save(any());
    }

    @Test
    void createRefusesBranchWithoutOrganisation() {
        ClassDefinitionDTO request = sampleClassDefinition(null, BRANCH_UUID, LocationType.ONLINE, null, null, null);

        assertThatThrownBy(() -> service.createClassDefinition(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("branch_uuid requires organisation_uuid");
        verify(classDefinitionRepository, never()).save(any());
    }

    @Test
    void createKeepsClientCoordinatesWithoutBranch() {
        ClassDefinitionDTO request = sampleClassDefinition(UUID.randomUUID(), null, LocationType.IN_PERSON,
                "Community Hall", new BigDecimal("-4.043477"), new BigDecimal("39.668206"));
        AtomicReference<ClassDefinition> saved = stubSuccessfulCreate(request);

        service.createClassDefinition(request);

        assertThat(saved.get().getLocationName()).isEqualTo("Community Hall");
        assertThat(saved.get().getLocationLatitude()).isEqualTo(new BigDecimal("-4.043477"));
        assertThat(saved.get().getLocationLongitude()).isEqualTo(new BigDecimal("39.668206"));
        verifyNoInteractions(trainingBranchLookupService);
    }

    @Test
    void createFromJobKeepsTheJobsCopiedLocation() {
        ClassDefinitionDTO request = sampleClassDefinition(UUID.randomUUID(), BRANCH_UUID, LocationType.HYBRID,
                "Main Campus · Kasarani, Nairobi", BRANCH_LATITUDE, BRANCH_LONGITUDE)
                .withResourceLinks(null, UUID.randomUUID());
        AtomicReference<ClassDefinition> saved = stubSuccessfulCreate(request);

        service.createClassDefinition(request);

        assertThat(saved.get().getLocationName()).isEqualTo("Main Campus · Kasarani, Nairobi");
        assertThat(saved.get().getLocationLatitude()).isEqualTo(BRANCH_LATITUDE);
        verify(trainingBranchLookupService, never()).findBranch(any(), any());
    }

    @Test
    void updateKeepsExistingCoordinatesWhenUnchangedBranchHasNoPin() {
        ClassDefinition existing = inPersonClassAtBranch();
        when(classDefinitionRepository.findByUuid(existing.getUuid())).thenReturn(Optional.of(existing));
        when(trainingBranchLookupService.findBranch(existing.getOrganisationUuid(), BRANCH_UUID)).thenReturn(Optional.of(
                new BranchLocation(BRANCH_UUID, existing.getOrganisationUuid(), "Main Campus", null, null, null, false)));
        when(classDefinitionRepository.save(any(ClassDefinition.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classSessionTemplateRepository.findByClassDefinitionUuidOrderByTemplateOrderAscCreatedDateAsc(existing.getUuid()))
                .thenReturn(List.of());

        service.updateClassDefinition(existing.getUuid(), updateRequest(null, "Renamed cohort"));

        assertThat(existing.getTitle()).isEqualTo("Renamed cohort");
        assertThat(existing.getLocationName()).isEqualTo("Main Campus · Kasarani, Nairobi");
        assertThat(existing.getLocationLatitude()).isEqualTo(BRANCH_LATITUDE);
        assertThat(existing.getLocationLongitude()).isEqualTo(BRANCH_LONGITUDE);
    }

    @Test
    void updateRefusesMovingToABranchWithoutPin() {
        UUID otherBranchUuid = UUID.randomUUID();
        ClassDefinition existing = inPersonClassAtBranch();
        when(classDefinitionRepository.findByUuid(existing.getUuid())).thenReturn(Optional.of(existing));
        when(trainingBranchLookupService.findBranch(existing.getOrganisationUuid(), otherBranchUuid)).thenReturn(Optional.of(
                new BranchLocation(otherBranchUuid, existing.getOrganisationUuid(), "Westlands Annex", null, null, null, true)));

        assertThatThrownBy(() -> service.updateClassDefinition(existing.getUuid(), updateRequest(otherBranchUuid, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training branch 'Westlands Annex' has no location pin; set it on the branch first");
        verify(classDefinitionRepository, never()).save(any());
    }

    @Test
    void createRefusesVenueFromAnotherBranch() {
        UUID organisationUuid = UUID.randomUUID();
        UUID venueUuid = UUID.randomUUID();
        ClassDefinitionDTO request = sampleClassDefinition(organisationUuid, BRANCH_UUID, LocationType.ONLINE,
                null, null, null).withResourceLinks(venueUuid, null);
        when(trainingBranchLookupService.findBranch(organisationUuid, BRANCH_UUID)).thenReturn(Optional.of(
                new BranchLocation(BRANCH_UUID, organisationUuid, "Main Campus", null, null, null, true)));
        when(resourceLookupService.getResource(venueUuid)).thenReturn(Optional.of(
                new apps.sarafrika.elimika.resourcing.spi.ResourceSummary(venueUuid, organisationUuid, UUID.randomUUID(),
                        apps.sarafrika.elimika.resourcing.spi.ResourceType.VENUE, "Physics Lab", 40, null, true)));

        assertThatThrownBy(() -> service.createClassDefinition(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Venue 'Physics Lab' is not at the class's branch");
        verify(classDefinitionRepository, never()).save(any());
    }

    private ClassDefinition inPersonClassAtBranch() {
        ClassDefinition entity = sampleClassDefinitionEntity();
        entity.setUuid(UUID.randomUUID());
        entity.setBranchUuid(BRANCH_UUID);
        entity.setLocationType(LocationType.IN_PERSON);
        entity.setLocationName("Main Campus · Kasarani, Nairobi");
        entity.setLocationLatitude(BRANCH_LATITUDE);
        entity.setLocationLongitude(BRANCH_LONGITUDE);
        return entity;
    }

    private ClassDefinitionDTO updateRequest(UUID branchUuid, String title) {
        return new ClassDefinitionDTO(null, title, null, null, null, branchUuid, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    private AtomicReference<ClassDefinition> stubSuccessfulCreate(ClassDefinitionDTO request) {
        UUID classUuid = UUID.randomUUID();
        AtomicReference<ClassDefinition> saved = new AtomicReference<>();
        when(classDefinitionRepository.save(any(ClassDefinition.class))).thenAnswer(invocation -> {
            ClassDefinition entity = invocation.getArgument(0);
            entity.setUuid(classUuid);
            saved.set(entity);
            return entity;
        });
        lenient().when(classSessionTemplateRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(availabilityService.isInstructorAvailable(any(), any(), any())).thenReturn(true);
        lenient().when(timetableServiceProvider.getIfAvailable()).thenReturn(timetableService);
        lenient().when(classScheduleServiceProvider.getIfAvailable()).thenReturn(null);
        lenient().when(timetableService.hasInstructorConflict(any(UUID.class), any(ScheduleRequestDTO.class))).thenReturn(false);
        lenient().when(timetableService.scheduleClass(any(ScheduleRequestDTO.class))).thenReturn(sampleScheduledInstance(classUuid));
        return saved;
    }
}
