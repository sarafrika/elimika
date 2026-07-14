package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.classes.dto.*;
import apps.sarafrika.elimika.classes.factory.ClassDefinitionFactory;
import apps.sarafrika.elimika.classes.factory.ClassSessionTemplateFactory;
import apps.sarafrika.elimika.classes.model.ClassSchedulingConflict;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassSchedulingConflictRepository;
import apps.sarafrika.elimika.classes.repository.ClassSessionTemplateRepository;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.spi.ClassDefinitionService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.model.ClassDefinitionResource;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionResourceRepository;
import apps.sarafrika.elimika.classes.util.RecurrencePatterns;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingRequest;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingService;
import apps.sarafrika.elimika.resourcing.spi.ResourceLookupService;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.utils.recurrence.OccurrenceWindow;
import apps.sarafrika.elimika.shared.utils.recurrence.RecurrenceExpander;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionDeactivatedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionUpdatedEventDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.ClassScheduleService;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import apps.sarafrika.elimika.shared.storage.service.MediaUploadRequest;
import apps.sarafrika.elimika.shared.storage.service.MediaValidationService;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import apps.sarafrika.elimika.shared.storage.util.MediaOwnerType;
import apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClassDefinitionServiceImpl implements ClassDefinitionServiceInterface, ClassDefinitionService {

    private final ClassDefinitionRepository classDefinitionRepository;
    private final ClassSchedulingConflictRepository classSchedulingConflictRepository;
    private final ClassSessionTemplateRepository classSessionTemplateRepository;
    private final ClassDefinitionResourceRepository classDefinitionResourceRepository;
    private final ResourceBookingService resourceBookingService;
    private final ResourceLookupService resourceLookupService;
    private final AvailabilityService availabilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final CourseInfoService courseInfoService;
    private final CourseTrainingApprovalSpi courseTrainingApprovalSpi;
    private final ObjectProvider<TimetableService> timetableServiceProvider;
    private final ObjectProvider<ClassScheduleService> classScheduleServiceProvider;
    private final MediaStorageService mediaStorageService;
    private final MediaValidationService mediaValidationService;
    private final StorageProperties storageProperties;

    private static final String CLASS_DEFINITION_NOT_FOUND_TEMPLATE = "Class definition with UUID %s not found";
    private static final String TRAINING_PROGRAM_NOT_FOUND_TEMPLATE = "Training program with UUID %s not found";
    private static final int MAX_ROLLOVER_ITERATIONS = 20;

    @Override
    public ClassDefinitionResponseDTO createClassDefinition(ClassDefinitionDTO classDefinitionDTO) {
        log.debug("Creating class definition with title: {}", classDefinitionDTO.title());

        if (classDefinitionDTO.sessionTemplates() == null || classDefinitionDTO.sessionTemplates().isEmpty()) {
            throw new IllegalArgumentException("At least one session template must be provided");
        }
        
        ClassDefinition entity = ClassDefinitionFactory.toEntity(classDefinitionDTO);
        
        // Validate instructor availability
        validateInstructorAvailability(entity.getDefaultInstructorUuid());
        
        // Set defaults
        if (entity.getMaxParticipants() == null) {
            entity.setMaxParticipants(50);
        }
        if (entity.getAllowWaitlist() == null) {
            entity.setAllowWaitlist(true);
        }
        if (entity.getIsActive() == null) {
            entity.setIsActive(true);
        }

        validateLocationRequirements(entity);
        validateLearningContext(entity);
        validateTrainingApprovals(entity);
        validateTrainingFee(entity);
        validateVenueCapacity(entity);

        ClassDefinition savedEntity = classDefinitionRepository.save(entity);
        List<ClassSessionTemplateDTO> persistedTemplates = saveSessionTemplates(
                savedEntity.getUuid(),
                classDefinitionDTO.sessionTemplates());
        ClassDefinitionDTO result = ClassDefinitionFactory.toDTO(savedEntity)
                .withSessionTemplates(persistedTemplates);

        ClassSchedulingOutcome schedulingOutcome = applySessionTemplates(result, persistedTemplates);
        persistSchedulingConflicts(result.uuid(), schedulingOutcome.conflicts());
        if (schedulingOutcome.blockingConflict()) {
            throw new SchedulingConflictException(
                    String.format("Conflicts detected for class %s", result.title()),
                    schedulingOutcome.conflicts());
        }
        bookVenueForInstances(result, schedulingOutcome.scheduledInstances());
        
        // Publish domain event
        ClassDefinedEventDTO event = new ClassDefinedEventDTO(
                result.uuid(),
                result.title(),
                (int) result.getDurationMinutes(),
                result.defaultInstructorUuid(),
                result.courseUuid(),
                result.organisationUuid(),
                result.locationType(),
                result.maxParticipants(),
                result.allowWaitlist(),
                null
        );
        eventPublisher.publishEvent(event);
        
        log.info("Created class definition with UUID: {} and published ClassDefinedEvent", result.uuid());
        return buildResponse(result);
    }

    @Override
    public ClassDefinitionResponseDTO createClassDefinition(ClassDefinitionDTO classDefinitionDTO,
                                                           MultipartFile thumbnail,
                                                           MultipartFile promotionalVideo) {
        if (!hasFile(thumbnail) && !hasFile(promotionalVideo)) {
            return createClassDefinition(classDefinitionDTO);
        }

        // Validate before creating the definition so an invalid file doesn't leave a
        // half-created class behind.
        if (hasFile(thumbnail)) {
            mediaValidationService.validate(thumbnail, MediaCategory.THUMBNAIL);
        }
        if (hasFile(promotionalVideo)) {
            mediaValidationService.validate(promotionalVideo, MediaCategory.VIDEO);
        }

        ClassDefinitionResponseDTO response = createClassDefinition(classDefinitionDTO);
        UUID definitionUuid = response.classDefinition().uuid();

        if (hasFile(thumbnail)) {
            response = uploadThumbnailValidated(definitionUuid, thumbnail);
        }
        if (hasFile(promotionalVideo)) {
            response = uploadPromotionalVideoValidated(definitionUuid, promotionalVideo);
        }

        return response;
    }

    @Override
    public ClassSessionTemplateScheduleResponseDTO addSessionTemplate(UUID definitionUuid,
                                                                     ClassSessionTemplateDTO sessionTemplate) {
        log.debug("Adding session template to class definition with UUID: {}", definitionUuid);

        if (sessionTemplate == null) {
            throw new IllegalArgumentException("Session template cannot be null");
        }

        ClassDefinitionDTO classDefinition = getClassDefinitionDTO(definitionUuid);
        ClassSchedulingOutcome schedulingOutcome = applySessionTemplates(classDefinition, List.of(sessionTemplate));
        persistSchedulingConflicts(definitionUuid, schedulingOutcome.conflicts());
        if (schedulingOutcome.blockingConflict()) {
            throw new SchedulingConflictException(
                    String.format("Conflicts detected for class %s", classDefinition.title()),
                    schedulingOutcome.conflicts());
        }

        bookClassResourcesForInstances(definitionUuid, schedulingOutcome.scheduledInstances());
        bookVenueForInstances(classDefinition, schedulingOutcome.scheduledInstances());

        ClassSessionTemplateDTO persistedTemplate = saveSessionTemplate(definitionUuid, sessionTemplate);
        return new ClassSessionTemplateScheduleResponseDTO(
                definitionUuid,
                persistedTemplate,
                schedulingOutcome.scheduledInstances(),
                schedulingOutcome.conflicts()
        );
    }

    /**
     * Books the class's copied resources (from its source marketplace job) for
     * instances scheduled after class creation. No-op for classes without copied
     * resources; validation failures surface as ResourceBookingConflictException.
     */
    private void bookClassResourcesForInstances(UUID classDefinitionUuid, List<ScheduledInstanceDTO> instances) {
        if (instances == null || instances.isEmpty()) {
            return;
        }
        List<ClassDefinitionResource> resources =
                classDefinitionResourceRepository.findByClassDefinitionUuidOrderByCreatedDateAsc(classDefinitionUuid);
        if (resources.isEmpty()) {
            return;
        }
        List<ResourceBookingRequest> requests = resources.stream()
                .map(resource -> new ResourceBookingRequest(
                        resource.getResourceUuid(),
                        resource.getQuantity() == null ? 1 : resource.getQuantity(),
                        List.of()))
                .toList();
        for (ScheduledInstanceDTO instance : instances) {
            resourceBookingService.createConfirmedBookingsForInstance(
                    classDefinitionUuid, instance.uuid(), instance.startTime(), instance.endTime(), requests);
        }
    }

    /**
     * Books the venue for a directly created class (not from a marketplace job — job
     * classes get their venue via hold conversion) that has no copied resource rows.
     */
    private void bookVenueForInstances(ClassDefinitionDTO classDefinition, List<ScheduledInstanceDTO> instances) {
        if (classDefinition.venueResourceUuid() == null
                || classDefinition.marketplaceJobUuid() != null
                || instances == null || instances.isEmpty()) {
            return;
        }
        boolean venueAlreadyCopied = classDefinitionResourceRepository
                .findByClassDefinitionUuidOrderByCreatedDateAsc(classDefinition.uuid())
                .stream()
                .anyMatch(resource -> classDefinition.venueResourceUuid().equals(resource.getResourceUuid()));
        if (venueAlreadyCopied) {
            return;
        }
        List<ResourceBookingRequest> venueRequest = List.of(
                new ResourceBookingRequest(classDefinition.venueResourceUuid(), 1, List.of()));
        for (ScheduledInstanceDTO instance : instances) {
            resourceBookingService.createConfirmedBookingsForInstance(
                    classDefinition.uuid(), instance.uuid(), instance.startTime(), instance.endTime(), venueRequest);
        }
    }

    /**
     * A class using a managed venue cannot admit more participants than the venue seats.
     */
    private void validateVenueCapacity(ClassDefinition entity) {
        if (entity.getVenueResourceUuid() == null) {
            return;
        }
        resourceLookupService.getResource(entity.getVenueResourceUuid()).ifPresent(summary -> {
            if (summary.seatCapacity() != null && entity.getMaxParticipants() != null
                    && entity.getMaxParticipants() > summary.seatCapacity()) {
                throw new IllegalArgumentException(String.format(
                        "max_participants %d exceeds the seat capacity %d of venue '%s'",
                        entity.getMaxParticipants(), summary.seatCapacity(), summary.name()));
            }
        });
    }

    private ClassSchedulingOutcome applySessionTemplates(ClassDefinitionDTO classDefinition,
                                                         List<ClassSessionTemplateDTO> templates) {
        List<ScheduledInstanceDTO> scheduledInstances = new ArrayList<>();
        List<ClassSchedulingConflictDTO> conflicts = new ArrayList<>();
        boolean blockingConflict = false;

        for (ClassSessionTemplateDTO template : templates) {
            if (template == null) {
                continue;
            }

            ConflictResolutionStrategy strategy = Optional.ofNullable(template.conflictResolution())
                    .orElse(ConflictResolutionStrategy.FAIL);
            int conflictCountBefore = conflicts.size();

            List<OccurrenceWindow> windows = RecurrenceExpander.expand(
                    template.startTime(),
                    template.endTime(),
                    RecurrencePatterns.fromRecurrenceDTO(template.recurrence()));

            for (OccurrenceWindow window : windows) {
                boolean scheduled = attemptScheduleWindow(classDefinition, window.start(), window.end(), conflicts, scheduledInstances);
                if (!scheduled && strategy == ConflictResolutionStrategy.ROLLOVER) {
                    attemptRollover(classDefinition, window.start(), window.end(), template.recurrence(), conflicts, scheduledInstances);
                }
            }

            if (strategy == ConflictResolutionStrategy.FAIL && conflicts.size() > conflictCountBefore) {
                blockingConflict = true;
            }
        }

        return new ClassSchedulingOutcome(scheduledInstances, conflicts, blockingConflict);
    }

    private boolean attemptScheduleWindow(ClassDefinitionDTO classDefinition,
                                          LocalDateTime start,
                                          LocalDateTime end,
                                          List<ClassSchedulingConflictDTO> conflicts,
                                          List<ScheduledInstanceDTO> scheduledInstances) {
        List<String> reasons = detectConflicts(classDefinition, start, end);
        if (reasons.isEmpty()) {
            scheduledInstances.add(scheduleInstance(classDefinition, start, end));
            return true;
        }

        conflicts.add(new ClassSchedulingConflictDTO(start, end, reasons));
        return false;
    }

    private boolean attemptRollover(ClassDefinitionDTO classDefinition,
                                    LocalDateTime start,
                                    LocalDateTime end,
                                    ClassRecurrenceDTO recurrence,
                                    List<ClassSchedulingConflictDTO> conflicts,
                                    List<ScheduledInstanceDTO> scheduledInstances) {
        ClassRecurrenceDTO safeRecurrence = recurrence != null ? recurrence :
                new ClassRecurrenceDTO(ClassRecurrenceDTO.RecurrenceType.DAILY, 1, null, null, null, 1);
        LocalDateTime rollingStart = start;
        LocalDateTime rollingEnd = end;
        int attempts = 0;

        while (attempts < MAX_ROLLOVER_ITERATIONS) {
            attempts++;
            rollingStart = advanceByRecurrence(rollingStart, safeRecurrence);
            rollingEnd = advanceByRecurrence(rollingEnd, safeRecurrence);

            List<String> reasons = detectConflicts(classDefinition, rollingStart, rollingEnd);
            if (reasons.isEmpty()) {
                scheduledInstances.add(scheduleInstance(classDefinition, rollingStart, rollingEnd));
                return true;
            }
            conflicts.add(new ClassSchedulingConflictDTO(rollingStart, rollingEnd, reasons));
        }
        return false;
    }

    private LocalDateTime advanceByRecurrence(LocalDateTime current, ClassRecurrenceDTO recurrence) {
        int interval = Optional.ofNullable(recurrence.intervalValue()).orElse(1);
        return switch (recurrence.recurrenceType()) {
            case DAILY -> current.plusDays(interval);
            case WEEKLY -> current.plusWeeks(interval);
            case MONTHLY -> current.plusMonths(interval);
            default -> current.plusDays(interval);
        };
    }

    private List<String> detectConflicts(ClassDefinitionDTO classDefinition, LocalDateTime start, LocalDateTime end) {
        List<String> reasons = new ArrayList<>();
        UUID instructorUuid = classDefinition.defaultInstructorUuid();

        if (!availabilityService.isInstructorAvailable(instructorUuid, start, end)) {
            reasons.add("Instructor is not available for the requested time range");
        }
        ScheduleRequestDTO requestDTO = new ScheduleRequestDTO(
                classDefinition.uuid(),
                instructorUuid,
                start,
                end,
                "UTC"
        );
        if (timetableService().hasInstructorConflict(instructorUuid, requestDTO)) {
            reasons.add("Instructor has overlapping scheduled instances");
        }
        if (classDefinition.venueResourceUuid() != null) {
            resourceBookingService.findConflicts(
                            classDefinition.venueResourceUuid(),
                            1,
                            start,
                            end,
                            classDefinition.marketplaceJobUuid(),
                            classDefinition.uuid())
                    .forEach(conflict -> reasons.add("Venue conflict: " + conflict.description()));
        }
        return reasons;
    }

    private ScheduledInstanceDTO scheduleInstance(ClassDefinitionDTO classDefinition, LocalDateTime start, LocalDateTime end) {
        ScheduleRequestDTO scheduleRequestDTO = new ScheduleRequestDTO(
                classDefinition.uuid(),
                classDefinition.defaultInstructorUuid(),
                start,
                end,
                "UTC"
        );
        return timetableService().scheduleClass(scheduleRequestDTO);
    }

    private record ClassSchedulingOutcome(List<ScheduledInstanceDTO> scheduledInstances,
                                          List<ClassSchedulingConflictDTO> conflicts,
                                          boolean blockingConflict) {
    }

    private TimetableService timetableService() {
        TimetableService svc = timetableServiceProvider.getIfAvailable();
        if (svc == null) {
            throw new IllegalStateException("TimetableService is not available");
        }
        return svc;
    }

    private ClassDefinitionResponseDTO buildResponse(ClassDefinitionDTO classDefinition) {
        return new ClassDefinitionResponseDTO(enrichWithScheduleProgress(classDefinition));
    }

    private ClassDefinitionDTO enrichWithScheduleProgress(ClassDefinitionDTO classDefinition) {
        if (classDefinition == null || classDefinition.uuid() == null) {
            return classDefinition;
        }
        ClassScheduleService scheduleService = classScheduleServiceProvider.getIfAvailable();
        if (scheduleService == null) {
            return classDefinition;
        }
        ClassScheduleService.ClassScheduleSummary summary = scheduleService.getScheduleSummary(classDefinition.uuid());
        if (summary == null) {
            return classDefinition;
        }
        return classDefinition.withScheduleProgress(
                summary.scheduledInstances(),
                summary.completedSessions(),
                summary.classProgressPercentage()
        );
    }

    private boolean isConflictResolved(ClassDefinitionDTO classDefinition,
                                       ClassSchedulingConflict conflict,
                                       List<ScheduledInstanceDTO> scheduledInstances) {
        if (conflict == null) {
            return true;
        }
        LocalDateTime requestedStart = conflict.getRequestedStart();
        LocalDateTime requestedEnd = conflict.getRequestedEnd();

        if (requestedStart != null && requestedEnd != null && scheduledInstances != null) {
            boolean scheduledMatch = scheduledInstances.stream()
                    .filter(instance -> instance != null)
                    .anyMatch(instance -> requestedStart.equals(instance.startTime())
                            && requestedEnd.equals(instance.endTime()));
            if (scheduledMatch) {
                return true;
            }
        }

        if (requestedStart == null || requestedEnd == null) {
            return true;
        }

        List<String> reasons = detectConflicts(classDefinition, requestedStart, requestedEnd);
        return reasons.isEmpty();
    }

    private ClassSchedulingConflictDTO toConflictDTO(ClassSchedulingConflict conflict) {
        List<String> reasons = conflict.getReasons() == null
                ? List.of()
                : Arrays.asList(conflict.getReasons());
        return new ClassSchedulingConflictDTO(
                conflict.getRequestedStart(),
                conflict.getRequestedEnd(),
                reasons
        );
    }

    private void resolveConflictsForClass(ClassDefinitionDTO classDefinition,
                                          List<ScheduledInstanceDTO> scheduledInstances) {
        if (classDefinition == null || classDefinition.uuid() == null) {
            return;
        }

        List<ClassSchedulingConflict> conflicts = classSchedulingConflictRepository
                .findByClassDefinitionUuidAndIsResolvedFalseOrderByRequestedStartAsc(classDefinition.uuid());
        if (conflicts.isEmpty()) {
            return;
        }

        List<ClassSchedulingConflict> resolvedConflicts = new ArrayList<>();
        for (ClassSchedulingConflict conflict : conflicts) {
            if (isConflictResolved(classDefinition, conflict, scheduledInstances)) {
                conflict.setIsResolved(true);
                conflict.setResolvedAt(LocalDateTime.now());
                resolvedConflicts.add(conflict);
            }
        }

        if (!resolvedConflicts.isEmpty()) {
            classSchedulingConflictRepository.saveAll(resolvedConflicts);
        }
    }

    private void ensureSessionTemplatesProvided(UUID classDefinitionUuid, boolean hasScheduledInstances) {
        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }
        boolean hasConflicts = classSchedulingConflictRepository.existsByClassDefinitionUuid(classDefinitionUuid);
        if (!hasScheduledInstances && !hasConflicts) {
            throw new IllegalStateException("Class schedule is unavailable because no session templates were provided when the class was created");
        }
    }

    private ClassDefinitionDTO getClassDefinitionDTO(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .map(this::toDTOWithSessionTemplates)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CLASS_DEFINITION_NOT_FOUND_TEMPLATE, classDefinitionUuid)));
    }

    private ClassDefinition requireClassDefinition(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CLASS_DEFINITION_NOT_FOUND_TEMPLATE, classDefinitionUuid)));
    }

    private ClassDefinitionDTO toDTOWithSessionTemplates(ClassDefinition entity) {
        ClassDefinitionDTO dto = ClassDefinitionFactory.toDTO(entity);
        if (dto == null || dto.uuid() == null) {
            return dto;
        }
        return dto.withSessionTemplates(loadSessionTemplates(dto.uuid()));
    }

    private List<ClassSessionTemplateDTO> loadSessionTemplates(UUID classDefinitionUuid) {
        return ClassSessionTemplateFactory.toDTOList(classSessionTemplateRepository
                .findByClassDefinitionUuidOrderByTemplateOrderAscCreatedDateAsc(classDefinitionUuid));
    }

    private List<ClassSessionTemplateDTO> saveSessionTemplates(UUID classDefinitionUuid,
                                                               List<ClassSessionTemplateDTO> sessionTemplates) {
        if (sessionTemplates == null || sessionTemplates.isEmpty()) {
            return List.of();
        }
        List<ClassSessionTemplate> entities = new ArrayList<>();
        for (int i = 0; i < sessionTemplates.size(); i++) {
            entities.add(ClassSessionTemplateFactory.toEntity(classDefinitionUuid, sessionTemplates.get(i), i));
        }
        return ClassSessionTemplateFactory.toDTOList(classSessionTemplateRepository.saveAll(entities));
    }

    private ClassSessionTemplateDTO saveSessionTemplate(UUID classDefinitionUuid,
                                                        ClassSessionTemplateDTO sessionTemplate) {
        int templateOrder = Math.toIntExact(classSessionTemplateRepository.countByClassDefinitionUuid(classDefinitionUuid));
        ClassSessionTemplate entity = ClassSessionTemplateFactory.toEntity(classDefinitionUuid, sessionTemplate, templateOrder);
        return ClassSessionTemplateFactory.toDTO(classSessionTemplateRepository.save(entity));
    }

    /**
     * Stores a class media file through the shared facade, returning the canonical
     * storage key and cleaning up the file it replaces.
     */
    private String storeClassMedia(MultipartFile file, MediaCategory category, String folder,
                                   String ownerType, UUID ownerUuid, String previousValue) {
        return mediaStorageService.store(new MediaUploadRequest(
                file, category, folder, ownerType, ownerUuid, previousValue)).key();
    }

    private void publishClassDefinitionUpdated(ClassDefinitionDTO result) {
        ClassDefinitionUpdatedEventDTO event = new ClassDefinitionUpdatedEventDTO(
                result.uuid(),
                result.title()
        );
        eventPublisher.publishEvent(event);
    }

    private void persistSchedulingConflicts(UUID classDefinitionUuid, List<ClassSchedulingConflictDTO> conflicts) {
        if (classDefinitionUuid == null || conflicts == null || conflicts.isEmpty()) {
            return;
        }

        List<ClassSchedulingConflict> entities = conflicts.stream()
                .map(conflict -> {
                    ClassSchedulingConflict entity = new ClassSchedulingConflict();
                    entity.setClassDefinitionUuid(classDefinitionUuid);
                    entity.setRequestedStart(conflict.requestedStart());
                    entity.setRequestedEnd(conflict.requestedEnd());
                    List<String> reasons = conflict.reasons() == null ? List.of() : conflict.reasons();
                    entity.setReasons(reasons.toArray(new String[0]));
                    entity.setIsResolved(false);
                    entity.setResolvedAt(null);
                    return entity;
                })
                .toList();

        classSchedulingConflictRepository.saveAll(entities);
    }

    @Override
    public ClassDefinitionResponseDTO updateClassDefinition(UUID definitionUuid, ClassDefinitionDTO classDefinitionDTO) {
        log.debug("Updating class definition with UUID: {}", definitionUuid);
        
        ClassDefinition existingEntity = classDefinitionRepository.findByUuid(definitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CLASS_DEFINITION_NOT_FOUND_TEMPLATE, definitionUuid)));

        ClassDefinitionFactory.updateEntityFromDTO(existingEntity, classDefinitionDTO);
        applyLearningContextOverrides(existingEntity, classDefinitionDTO);
        validateLocationRequirements(existingEntity);
        validateLearningContext(existingEntity);
        validateTrainingApprovals(existingEntity);
        validateTrainingFee(existingEntity);
        validateVenueCapacity(existingEntity);
        
        ClassDefinition savedEntity = classDefinitionRepository.save(existingEntity);
        ClassDefinitionDTO result = toDTOWithSessionTemplates(savedEntity);
        
        publishClassDefinitionUpdated(result);
        
        log.info("Updated class definition with UUID: {} and published ClassDefinitionUpdatedEvent", definitionUuid);
        return buildResponse(result);
    }

    @Override
    public ClassDefinitionResponseDTO uploadThumbnail(UUID definitionUuid, MultipartFile thumbnail) {
        log.debug("Uploading thumbnail for class definition: {}", definitionUuid);

        return uploadThumbnailValidated(definitionUuid, thumbnail);
    }

    private ClassDefinitionResponseDTO uploadThumbnailValidated(UUID definitionUuid, MultipartFile thumbnail) {
        ClassDefinition entity = requireClassDefinition(definitionUuid);

        try {
            String folder = storageProperties.getFolders().getClassThumbnails() + "/" + definitionUuid;
            entity.setThumbnailUrl(storeClassMedia(thumbnail, MediaCategory.THUMBNAIL, folder,
                    MediaOwnerType.CLASS_THUMBNAIL, definitionUuid, entity.getThumbnailUrl()));
            ClassDefinition savedEntity = classDefinitionRepository.save(entity);
            ClassDefinitionDTO result = toDTOWithSessionTemplates(savedEntity);
            publishClassDefinitionUpdated(result);
            return buildResponse(result);
        } catch (Exception ex) {
            log.error("Failed to upload class thumbnail for UUID: {}", definitionUuid, ex);
            throw new RuntimeException("Failed to upload class thumbnail: " + ex.getMessage(), ex);
        }
    }

    @Override
    public ClassDefinitionResponseDTO uploadPromotionalVideo(UUID definitionUuid, MultipartFile promotionalVideo) {
        log.debug("Uploading promotional video for class definition: {}", definitionUuid);

        return uploadPromotionalVideoValidated(definitionUuid, promotionalVideo);
    }

    private ClassDefinitionResponseDTO uploadPromotionalVideoValidated(UUID definitionUuid, MultipartFile promotionalVideo) {
        ClassDefinition entity = requireClassDefinition(definitionUuid);

        try {
            String folder = storageProperties.getFolders().getClassPromotionalVideos() + "/" + definitionUuid;
            entity.setPromotionalVideoUrl(storeClassMedia(promotionalVideo, MediaCategory.VIDEO, folder,
                    MediaOwnerType.CLASS_PROMO_VIDEO, definitionUuid, entity.getPromotionalVideoUrl()));
            ClassDefinition savedEntity = classDefinitionRepository.save(entity);
            ClassDefinitionDTO result = toDTOWithSessionTemplates(savedEntity);
            publishClassDefinitionUpdated(result);
            return buildResponse(result);
        } catch (Exception ex) {
            log.error("Failed to upload class promotional video for UUID: {}", definitionUuid, ex);
            throw new RuntimeException("Failed to upload class promotional video: " + ex.getMessage(), ex);
        }
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    @Override
    public void deactivateClassDefinition(UUID definitionUuid) {
        log.debug("Deactivating class definition with UUID: {}", definitionUuid);
        
        ClassDefinition entity = classDefinitionRepository.findByUuid(definitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CLASS_DEFINITION_NOT_FOUND_TEMPLATE, definitionUuid)));
        
        String title = entity.getTitle();
        entity.setIsActive(false);
        classDefinitionRepository.save(entity);
        
        // Publish domain event
        ClassDefinitionDeactivatedEventDTO event = new ClassDefinitionDeactivatedEventDTO(
                definitionUuid,
                title
        );
        eventPublisher.publishEvent(event);
        
        log.info("Deactivated class definition with UUID: {} and published ClassDefinitionDeactivatedEvent", definitionUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassDefinitionResponseDTO getClassDefinition(UUID definitionUuid) {
        log.debug("Retrieving class definition with UUID: {}", definitionUuid);

        ClassDefinitionDTO classDefinition = classDefinitionRepository.findByUuid(definitionUuid)
                .map(this::toDTOWithSessionTemplates)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CLASS_DEFINITION_NOT_FOUND_TEMPLATE, definitionUuid)));
        return buildResponse(classDefinition);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionResponseDTO> findClassesForCourse(UUID courseUuid) {
        log.debug("Finding classes for course UUID: {}", courseUuid);

        return classDefinitionRepository.findByCourseUuid(courseUuid)
                .stream()
                .map(this::toDTOWithSessionTemplates)
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionResponseDTO> findActiveClassesForCourse(UUID courseUuid) {
        log.debug("Finding active classes for course UUID: {}", courseUuid);

        if (!courseInfoService.isCourseApproved(courseUuid)) {
            log.debug("Course {} is not approved; returning no active classes", courseUuid);
            return List.of();
        }

        return classDefinitionRepository.findActiveClassesForCourse(courseUuid)
                .stream()
                .map(this::toDTOWithSessionTemplates)
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionResponseDTO> findClassesForProgram(UUID programUuid) {
        log.debug("Finding classes for program UUID: {}", programUuid);

        return classDefinitionRepository.findByProgramUuid(programUuid)
                .stream()
                .map(this::toDTOWithSessionTemplates)
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionResponseDTO> findActiveClassesForProgram(UUID programUuid) {
        log.debug("Finding active classes for program UUID: {}", programUuid);

        if (!courseInfoService.isTrainingProgramApproved(programUuid)) {
            log.debug("Training program {} is not approved; returning no active classes", programUuid);
            return List.of();
        }

        return classDefinitionRepository.findActiveClassesForProgram(programUuid)
                .stream()
                .map(this::toDTOWithSessionTemplates)
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionResponseDTO> findClassesForInstructor(UUID instructorUuid) {
        log.debug("Finding classes for instructor UUID: {}", instructorUuid);

        return classDefinitionRepository.findByDefaultInstructorUuid(instructorUuid)
                .stream()
                .map(this::toDTOWithSessionTemplates)
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionResponseDTO> findActiveClassesForInstructor(UUID instructorUuid) {
        log.debug("Finding active classes for instructor UUID: {}", instructorUuid);

        return classDefinitionRepository.findActiveClassesForInstructor(instructorUuid)
                .stream()
                .filter(this::isLinkedContentApproved)
                .map(this::toDTOWithSessionTemplates)
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionResponseDTO> findClassesForOrganisation(UUID organisationUuid) {
        log.debug("Finding classes for organisation UUID: {}", organisationUuid);

        return classDefinitionRepository.findByOrganisationUuid(organisationUuid)
                .stream()
                .map(this::toDTOWithSessionTemplates)
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganisationInstructorPayableDTO> getInstructorPayablesForOrganisation(UUID organisationUuid) {
        log.debug("Computing instructor payables for organisation UUID: {}", organisationUuid);

        record Accumulator(BigDecimal amount, long classes, long sessions) {}
        Map<UUID, Accumulator> byInstructor = new LinkedHashMap<>();

        for (ClassDefinitionResponseDTO response : findClassesForOrganisation(organisationUuid)) {
            ClassDefinitionDTO definition = response.classDefinition();
            if (definition == null || definition.defaultInstructorUuid() == null) {
                continue;
            }
            BigDecimal fee = definition.trainingFee() == null ? BigDecimal.ZERO : definition.trainingFee();
            long completedSessions = definition.completedSessionCount() == null
                    ? 0L
                    : definition.completedSessionCount();
            BigDecimal owed = fee.multiply(BigDecimal.valueOf(completedSessions));

            byInstructor.merge(
                    definition.defaultInstructorUuid(),
                    new Accumulator(owed, 1L, completedSessions),
                    (existing, added) -> new Accumulator(
                            existing.amount().add(added.amount()),
                            existing.classes() + added.classes(),
                            existing.sessions() + added.sessions()));
        }

        return byInstructor.entrySet().stream()
                .map(entry -> new OrganisationInstructorPayableDTO(
                        entry.getKey(),
                        entry.getValue().amount(),
                        entry.getValue().classes(),
                        entry.getValue().sessions()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassDefinitionResponseDTO> findAllClasses(Pageable pageable) {
        log.debug("Finding all classes (page: {}, size: {})", pageable.getPageNumber(), pageable.getPageSize());

        return classDefinitionRepository.findAll(pageable)
                .map(this::toDTOWithSessionTemplates)
                .map(this::buildResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionResponseDTO> findAllActiveClasses() {
        log.debug("Finding all active classes");

        return classDefinitionRepository.findByIsActiveTrue()
                .stream()
                .filter(this::isLinkedContentApproved)
                .map(this::toDTOWithSessionTemplates)
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInstructorAvailability(UUID instructorUuid) {
        log.debug("Checking if instructor {} has availability defined", instructorUuid);
        
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }

        try {
            var availability = availabilityService.getAvailabilityForInstructor(instructorUuid);
            boolean hasAvailability = !availability.isEmpty();
            log.debug("Instructor {} availability check: {}", instructorUuid, hasAvailability ? "HAS availability" : "NO availability");
            return hasAvailability;
        } catch (Exception e) {
            log.warn("Error checking availability for instructor {}: {}", instructorUuid, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScheduledInstanceDTO> getClassSchedule(UUID classDefinitionUuid, Pageable pageable) {
        log.debug("Fetching schedule for class definition {}", classDefinitionUuid);

        getClassDefinitionDTO(classDefinitionUuid);
        Page<ScheduledInstanceDTO> page = timetableService()
                .getScheduledInstancesForClassDefinition(classDefinitionUuid, pageable);
        ensureSessionTemplatesProvided(classDefinitionUuid, page.getTotalElements() > 0);
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassSchedulingConflictDTO> getSchedulingConflicts(UUID classDefinitionUuid, Pageable pageable) {
        log.debug("Fetching scheduling conflicts for class definition {}", classDefinitionUuid);

        ClassDefinitionDTO classDefinition = getClassDefinitionDTO(classDefinitionUuid);
        List<ScheduledInstanceDTO> scheduledInstances = timetableService()
                .getScheduledInstancesForClassDefinition(classDefinitionUuid);

        resolveConflictsForClass(classDefinition, scheduledInstances);

        Page<ClassSchedulingConflictDTO> page = classSchedulingConflictRepository
                .findByClassDefinitionUuidAndIsResolvedFalseOrderByRequestedStartAsc(classDefinitionUuid, pageable)
                .map(this::toConflictDTO);
        ensureSessionTemplatesProvided(classDefinitionUuid, !scheduledInstances.isEmpty());
        return page;
    }

    /**
     * Validates that an instructor has availability defined before creating a class.
     * Logs a warning if no availability is found but doesn't prevent class creation.
     *
     * @param instructorUuid The UUID of the instructor to validate
     */
    private void validateInstructorAvailability(UUID instructorUuid) {
        try {
            var availability = availabilityService.getAvailabilityForInstructor(instructorUuid);
            if (availability.isEmpty()) {
                log.warn("Instructor {} has no availability defined. Consider setting availability patterns before scheduling classes.", instructorUuid);
            } else {
                log.debug("Instructor {} has {} availability slots defined", instructorUuid, availability.size());
            }
        } catch (Exception e) {
            log.warn("Could not validate availability for instructor {}: {}", instructorUuid, e.getMessage());
        }
    }

    private Optional<BigDecimal> resolveApprovedRate(ClassDefinition entity) {
        UUID courseUuid = entity.getCourseUuid();
        if (courseUuid == null) {
            return Optional.empty();
        }

        SessionFormat sessionFormat = entity.getSessionFormat();
        LocationType locationType = entity.getLocationType();

        if (entity.getDefaultInstructorUuid() != null) {
            Optional<BigDecimal> instructorRate = courseTrainingApprovalSpi.resolveInstructorRate(
                    courseUuid,
                    entity.getDefaultInstructorUuid(),
                    sessionFormat,
                    locationType
            );
            if (instructorRate.isPresent()) {
                return instructorRate;
            }
        }

        if (entity.getOrganisationUuid() != null) {
            return courseTrainingApprovalSpi.resolveOrganisationRate(
                    courseUuid,
                    entity.getOrganisationUuid(),
                    sessionFormat,
                    locationType
            );
        }

        return Optional.empty();
    }

    private Optional<BigDecimal> resolveProgramApprovedRate(ClassDefinition entity) {
        UUID programUuid = entity.getProgramUuid();
        if (programUuid == null) {
            return Optional.empty();
        }

        SessionFormat sessionFormat = entity.getSessionFormat();
        LocationType locationType = entity.getLocationType();

        if (entity.getDefaultInstructorUuid() != null) {
            Optional<BigDecimal> instructorRate = courseTrainingApprovalSpi.resolveInstructorProgramRate(
                    programUuid,
                    entity.getDefaultInstructorUuid(),
                    sessionFormat,
                    locationType
            );
            if (instructorRate.isPresent()) {
                return instructorRate;
            }
        }

        if (entity.getOrganisationUuid() != null) {
            return courseTrainingApprovalSpi.resolveOrganisationProgramRate(
                    programUuid,
                    entity.getOrganisationUuid(),
                    sessionFormat,
                    locationType
            );
        }

        return Optional.empty();
    }

    private void validateTrainingFee(ClassDefinition entity) {
        UUID courseUuid = entity.getCourseUuid();
        UUID programUuid = entity.getProgramUuid();
        if (courseUuid == null && programUuid == null) {
            return;
        }

        if (entity.getClassVisibility() == null) {
            throw new IllegalArgumentException("Class visibility is required when linking a class definition to a course or training program");
        }
        if (entity.getSessionFormat() == null) {
            throw new IllegalArgumentException("Session format is required when linking a class definition to a course or training program");
        }
        if (entity.getLocationType() == null) {
            throw new IllegalArgumentException("Location type is required when linking a class definition to a course or training program");
        }

        if (courseUuid != null) {
            // Verify course exists and get minimum training fee via SPI
            BigDecimal minimumTrainingFee = courseInfoService.getMinimumTrainingFee(courseUuid)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format("Course with UUID %s not found", courseUuid)));

            BigDecimal resolvedRate = resolveApprovedRate(entity)
                    .orElseThrow(() -> new IllegalStateException(String.format(
                            "No approved rate card found for the selected instructor/organisation on course %s. Submit and approve a training application with rates first.",
                            courseUuid)));

            if (entity.getTrainingFee() == null) {
                entity.setTrainingFee(resolvedRate);
            } else if (entity.getTrainingFee().compareTo(resolvedRate) != 0) {
                throw new IllegalArgumentException(String.format(
                        "Training fee %.2f must match the approved rate card amount %.2f for %s %s delivery.",
                        entity.getTrainingFee(),
                        resolvedRate,
                        entity.getSessionFormat(),
                        entity.getLocationType()));
            }

            if (entity.getTrainingFee().compareTo(minimumTrainingFee) < 0) {
                throw new IllegalArgumentException(String.format(
                        "Training fee %.2f cannot be less than the course minimum training fee %.2f",
                        entity.getTrainingFee(), minimumTrainingFee));
            }
            return;
        }

        BigDecimal resolvedRate = resolveProgramApprovedRate(entity)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "No approved rate card found for the selected instructor/organisation on training program %s. Submit and approve a training application with rates first.",
                        programUuid)));

        if (entity.getTrainingFee() == null) {
            entity.setTrainingFee(resolvedRate);
        } else if (entity.getTrainingFee().compareTo(resolvedRate) != 0) {
            throw new IllegalArgumentException(String.format(
                    "Training fee %.2f must match the approved rate card amount %.2f for %s %s delivery.",
                    entity.getTrainingFee(),
                    resolvedRate,
                    entity.getSessionFormat(),
                    entity.getLocationType()));
        }
    }

    private void validateTrainingApprovals(ClassDefinition entity) {
        UUID courseUuid = entity.getCourseUuid();
        UUID programUuid = entity.getProgramUuid();
        if (courseUuid == null && programUuid == null) {
            return;
        }

        UUID instructorUuid = entity.getDefaultInstructorUuid();
        if (courseUuid != null
                && instructorUuid != null
                && !courseTrainingApprovalSpi.isInstructorApproved(courseUuid, instructorUuid)) {
            throw new IllegalStateException(String.format(
                    "Instructor %s is not approved to deliver course %s. Submit a training application and wait for approval before scheduling classes.",
                    instructorUuid, courseUuid));
        }
        if (programUuid != null
                && instructorUuid != null
                && !courseTrainingApprovalSpi.isInstructorApprovedForProgram(programUuid, instructorUuid)) {
            throw new IllegalStateException(String.format(
                    "Instructor %s is not approved to deliver training program %s. Submit a training application and wait for approval before scheduling classes.",
                    instructorUuid, programUuid));
        }

        UUID organisationUuid = entity.getOrganisationUuid();
        if (courseUuid != null
                && organisationUuid != null
                && !courseTrainingApprovalSpi.isOrganisationApproved(courseUuid, organisationUuid)) {
            throw new IllegalStateException(String.format(
                    "Organisation %s is not approved to deliver course %s. Submit a training application and wait for approval before scheduling classes.",
                    organisationUuid, courseUuid));
        }
        if (programUuid != null
                && organisationUuid != null
                && !courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, organisationUuid)) {
            throw new IllegalStateException(String.format(
                    "Organisation %s is not approved to deliver training program %s. Submit a training application and wait for approval before scheduling classes.",
                    organisationUuid, programUuid));
        }
    }

    private void validateLearningContext(ClassDefinition entity) {
        UUID courseUuid = entity.getCourseUuid();
        UUID programUuid = entity.getProgramUuid();

        if (courseUuid != null && programUuid != null) {
            throw new IllegalArgumentException("Class definition can be linked to either a course or a training program, not both.");
        }

        if (courseUuid != null && !courseInfoService.courseExists(courseUuid)) {
            throw new ResourceNotFoundException(String.format("Course with UUID %s not found", courseUuid));
        }

        if (courseUuid != null && !courseInfoService.isCourseApproved(courseUuid)) {
            throw new IllegalStateException(String.format(
                    "Course %s is not approved for delivery. An admin must approve it before instructors or organisations can schedule classes.",
                    courseUuid));
        }

        if (programUuid != null && !courseInfoService.trainingProgramExists(programUuid)) {
            throw new ResourceNotFoundException(String.format(TRAINING_PROGRAM_NOT_FOUND_TEMPLATE, programUuid));
        }

        if (programUuid != null && !courseInfoService.isTrainingProgramApproved(programUuid)) {
            throw new IllegalStateException(String.format(
                    "Training program %s is not approved for delivery. An admin must approve it before instructors or organisations can schedule classes.",
                    programUuid));
        }
    }

    private void applyLearningContextOverrides(ClassDefinition entity, ClassDefinitionDTO dto) {
        if (dto == null) {
            return;
        }

        if (dto.courseUuid() != null && dto.programUuid() != null) {
            throw new IllegalArgumentException("Class definition can be linked to either a course or a training program, not both.");
        }

        if (dto.courseUuid() != null) {
            entity.setCourseUuid(dto.courseUuid());
            entity.setProgramUuid(null);
            return;
        }

        if (dto.programUuid() != null) {
            entity.setProgramUuid(dto.programUuid());
            entity.setCourseUuid(null);
        }
    }

    private boolean isLinkedContentApproved(ClassDefinition classDefinition) {
        if (classDefinition == null) {
            return false;
        }

        UUID courseUuid = classDefinition.getCourseUuid();
        if (courseUuid != null) {
            return courseInfoService.isCourseApproved(courseUuid);
        }

        UUID programUuid = classDefinition.getProgramUuid();
        if (programUuid != null) {
            return courseInfoService.isTrainingProgramApproved(programUuid);
        }

        return true;
    }

    /**
     * Ensures that in-person and hybrid classes carry a Mapbox-ready location payload.
     * <p>
     * ONLINE classes may omit location_name and coordinates; IN_PERSON and HYBRID must supply:
     * - location_name (human readable)
     * - location_latitude (between -90 and 90)
     * - location_longitude (between -180 and 180)
     *
     * @param entity the class definition to validate
     */
    private void validateLocationRequirements(ClassDefinition entity) {
        LocationType locationType = entity.getLocationType();
        if (locationType == null) {
            return;
        }

        if (LocationType.ONLINE.equals(locationType)) {
            // Online classes do not require a physical location; location fields are optional
            return;
        }

        String locationName = entity.getLocationName();
        if (locationName == null || locationName.trim().isEmpty()) {
            throw new IllegalArgumentException("location_name is required when location_type is IN_PERSON or HYBRID");
        }

        var latitude = entity.getLocationLatitude();
        var longitude = entity.getLocationLongitude();
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("location_latitude and location_longitude are required when location_type is IN_PERSON or HYBRID");
        }

        if (latitude.compareTo(new BigDecimal("-90")) < 0 || latitude.compareTo(new BigDecimal("90")) > 0) {
            throw new IllegalArgumentException("location_latitude must be between -90 and 90 degrees");
        }
        if (longitude.compareTo(new BigDecimal("-180")) < 0 || longitude.compareTo(new BigDecimal("180")) > 0) {
            throw new IllegalArgumentException("location_longitude must be between -180 and 180 degrees");
        }
    }
}
