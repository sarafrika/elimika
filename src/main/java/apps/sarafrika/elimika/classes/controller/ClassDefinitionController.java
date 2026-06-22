package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionCreateRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionUpdateRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassSchedulingConflictDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateScheduleResponseDTO;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.shared.storage.util.StoragePathUtils;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Class Definition Management", description = "APIs for creating and managing class definitions and scheduling.")

public class ClassDefinitionController {

    private final ClassDefinitionServiceInterface classDefinitionService;
    private final TimetableService timetableService;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    // ================================
    // CORE CLASS DEFINITION MANAGEMENT
    // ================================

    @Operation(summary = "Create a new class definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Class definition created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClassDefinitionResponseDTO>> createClassDefinition(
            @Valid @RequestBody ClassDefinitionCreateRequestDTO request) {
        log.debug("REST request to create class definition: {}", request.title());

        try {
            ClassDefinitionResponseDTO result = classDefinitionService.createClassDefinition(request.toClassDefinitionDTO());
            return ResponseEntity.status(201).body(ApiResponse.success(result, "Class definition created successfully"));
        } catch (SchedulingConflictException e) {
            log.warn("Scheduling conflicts while creating class definition {}: {}", request.title(), e.getMessage());
            return ResponseEntity.status(409).body(ApiResponse.error("Scheduling conflicts detected", e.getConflicts()));
        }
    }

    @Operation(summary = "Create a new class definition with media")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Class definition created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClassDefinitionResponseDTO>> createClassDefinitionMultipart(
            @RequestPart(value = "request", required = false) String requestPart,
            @RequestPart(value = "class_definition", required = false) String classDefinitionPart,
            @RequestParam MultiValueMap<String, String> formFields,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "promotional_video", required = false) MultipartFile promotionalVideo,
            @RequestParam(value = "promotionalVideo", required = false) MultipartFile camelCasePromotionalVideo,
            @RequestParam(value = "marketing_video", required = false) MultipartFile marketingVideo,
            @RequestParam(value = "marketingVideo", required = false) MultipartFile camelCaseMarketingVideo) {
        ClassDefinitionCreateRequestDTO request = resolveMultipartCreateRequest(requestPart, classDefinitionPart, formFields);
        log.debug("REST multipart request to create class definition: {}", request.title());

        try {
            ClassDefinitionResponseDTO result = classDefinitionService.createClassDefinition(
                    request.toClassDefinitionDTO(),
                    fileOrNull(thumbnail),
                    fileOrNull(promotionalVideo, camelCasePromotionalVideo, marketingVideo, camelCaseMarketingVideo));
            return ResponseEntity.status(201).body(ApiResponse.success(result, "Class definition created successfully"));
        } catch (SchedulingConflictException e) {
            log.warn("Scheduling conflicts while creating class definition {}: {}", request.title(), e.getMessage());
            return ResponseEntity.status(409).body(ApiResponse.error("Scheduling conflicts detected", e.getConflicts()));
        }
    }

    @Operation(summary = "Create a new class definition for a training program")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Class definition created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping(value = "/program/{programUuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClassDefinitionResponseDTO>> createClassDefinitionForProgram(
            @Parameter(description = "UUID of the training program", required = true)
            @PathVariable UUID programUuid,
            @Valid @RequestBody ClassDefinitionCreateRequestDTO request) {
        log.debug("REST request to create class definition: {} for training program: {}", request.title(), programUuid);

        if (request.courseUuid() != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "course_uuid is not allowed when creating a class under /api/v1/classes/program/{programUuid}"));
        }

        try {
            ClassDefinitionResponseDTO result = classDefinitionService.createClassDefinition(
                    request.toClassDefinitionDTO(null, programUuid));
            return ResponseEntity.status(201).body(ApiResponse.success(result, "Class definition created successfully"));
        } catch (SchedulingConflictException e) {
            log.warn("Scheduling conflicts while creating class definition {} for program {}: {}",
                    request.title(), programUuid, e.getMessage());
            return ResponseEntity.status(409).body(ApiResponse.error("Scheduling conflicts detected", e.getConflicts()));
        }
    }

    @Operation(summary = "Create a new class definition with media for a training program")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Class definition created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping(value = "/program/{programUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClassDefinitionResponseDTO>> createClassDefinitionForProgramMultipart(
            @Parameter(description = "UUID of the training program", required = true)
            @PathVariable UUID programUuid,
            @RequestPart(value = "request", required = false) String requestPart,
            @RequestPart(value = "class_definition", required = false) String classDefinitionPart,
            @RequestParam MultiValueMap<String, String> formFields,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "promotional_video", required = false) MultipartFile promotionalVideo,
            @RequestParam(value = "promotionalVideo", required = false) MultipartFile camelCasePromotionalVideo,
            @RequestParam(value = "marketing_video", required = false) MultipartFile marketingVideo,
            @RequestParam(value = "marketingVideo", required = false) MultipartFile camelCaseMarketingVideo) {
        ClassDefinitionCreateRequestDTO request = resolveMultipartCreateRequest(requestPart, classDefinitionPart, formFields);
        log.debug("REST multipart request to create class definition: {} for training program: {}",
                request.title(), programUuid);

        if (request.courseUuid() != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "course_uuid is not allowed when creating a class under /api/v1/classes/program/{programUuid}"));
        }

        try {
            ClassDefinitionResponseDTO result = classDefinitionService.createClassDefinition(
                    request.toClassDefinitionDTO(null, programUuid),
                    fileOrNull(thumbnail),
                    fileOrNull(promotionalVideo, camelCasePromotionalVideo, marketingVideo, camelCaseMarketingVideo));
            return ResponseEntity.status(201).body(ApiResponse.success(result, "Class definition created successfully"));
        } catch (SchedulingConflictException e) {
            log.warn("Scheduling conflicts while creating class definition {} for program {}: {}",
                    request.title(), programUuid, e.getMessage());
            return ResponseEntity.status(409).body(ApiResponse.error("Scheduling conflicts detected", e.getConflicts()));
        }
    }

    @Operation(summary = "List enrollments for a class definition across all scheduled instances")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully")
    @GetMapping("/{uuid}/enrollments")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getEnrollmentsForClass(
            @Parameter(description = "UUID of the class definition", required = true)
            @PathVariable UUID uuid) {
        log.debug("REST request to get enrollments for class definition: {}", uuid);

        List<EnrollmentDTO> enrollments = timetableService.getEnrollmentsForClass(uuid);
        return ResponseEntity.ok(ApiResponse.success(enrollments, "Enrollments retrieved successfully"));
    }

    @Operation(summary = "Get a class definition by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definition retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ClassDefinitionResponseDTO>> getClassDefinition(
            @Parameter(description = "UUID of the class definition to retrieve", required = true)
            @PathVariable UUID uuid) {
        log.debug("REST request to get class definition: {}", uuid);
        
        ClassDefinitionResponseDTO result = classDefinitionService.getClassDefinition(uuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definition retrieved successfully"));
    }

    @Operation(summary = "Update a class definition by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definition updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ClassDefinitionResponseDTO>> updateClassDefinition(
            @Parameter(description = "UUID of the class definition to update", required = true)
            @PathVariable UUID uuid,
            @Valid @RequestBody ClassDefinitionUpdateRequestDTO request) {
        log.debug("REST request to update class definition: {}", uuid);
        
        ClassDefinitionResponseDTO result = classDefinitionService.updateClassDefinition(uuid, request.toClassDefinitionDTO());
        return ResponseEntity.ok(ApiResponse.success(result, "Class definition updated successfully"));
    }

    @Operation(summary = "Upload class thumbnail")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class thumbnail uploaded successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid thumbnail file")
    @PostMapping(value = "/{uuid}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClassDefinitionResponseDTO>> uploadClassThumbnail(
            @Parameter(description = "UUID of the class definition", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Thumbnail image file. Supported formats: JPG, PNG, GIF, WebP. Maximum size: 5MB.", required = true)
            @RequestParam("thumbnail") MultipartFile thumbnail) {
        log.debug("REST request to upload thumbnail for class definition: {}", uuid);

        ClassDefinitionResponseDTO result = classDefinitionService.uploadThumbnail(uuid, thumbnail);
        return ResponseEntity.ok(ApiResponse.success(result, "Class thumbnail uploaded successfully"));
    }

    @Operation(summary = "Upload class promotional video")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class promotional video uploaded successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid promotional video file")
    @PostMapping(value = "/{uuid}/promotional-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClassDefinitionResponseDTO>> uploadClassPromotionalVideo(
            @Parameter(description = "UUID of the class definition", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Promotional video file. Supported formats: MP4, WebM, MOV, AVI. Maximum size: 100MB.", required = true)
            @RequestParam("promotional_video") MultipartFile promotionalVideo) {
        log.debug("REST request to upload promotional video for class definition: {}", uuid);

        ClassDefinitionResponseDTO result = classDefinitionService.uploadPromotionalVideo(uuid, promotionalVideo);
        return ResponseEntity.ok(ApiResponse.success(result, "Class promotional video uploaded successfully"));
    }

    @Operation(summary = "Get uploaded class media")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class media retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class media not found")
    @GetMapping("/media/{*filePath}")
    public ResponseEntity<Resource> getClassMedia(
            @Parameter(description = "Stored relative path of the class media file.", required = true)
            @PathVariable String filePath) {
        try {
            String fullPath = resolveClassMediaPath(filePath);
            Resource resource = storageService.load(fullPath);
            String contentType = storageService.getContentType(fullPath);
            String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Add a session template to an existing class definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Class session template applied successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid session template")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Scheduling conflicts detected")
    @PostMapping("/{uuid}/session-templates")
    public ResponseEntity<ApiResponse<ClassSessionTemplateScheduleResponseDTO>> addSessionTemplate(
            @Parameter(description = "UUID of the class definition", required = true)
            @PathVariable UUID uuid,
            @Valid @RequestBody ClassSessionTemplateDTO request) {
        log.debug("REST request to add session template to class definition: {}", uuid);

        try {
            ClassSessionTemplateScheduleResponseDTO result = classDefinitionService.addSessionTemplate(uuid, request);
            return ResponseEntity.status(201).body(ApiResponse.success(result, "Class session template applied successfully"));
        } catch (SchedulingConflictException e) {
            log.warn("Scheduling conflicts while adding session template to class definition {}: {}", uuid, e.getMessage());
            return ResponseEntity.status(409).body(ApiResponse.error("Scheduling conflicts detected", e.getConflicts()));
        }
    }

    @Operation(summary = "Deactivate a class definition by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definition deactivated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deactivateClassDefinition(
            @Parameter(description = "UUID of the class definition to deactivate", required = true)
            @PathVariable UUID uuid) {
        log.debug("REST request to deactivate class definition: {}", uuid);
        
        classDefinitionService.deactivateClassDefinition(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Class definition deactivated successfully"));
    }

    // ================================
    // CLASS DEFINITION QUERIES
    // ================================

    @Operation(summary = "Get class definitions for a course")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definitions retrieved successfully")
    @GetMapping("/course/{courseUuid}")
    public ResponseEntity<ApiResponse<List<ClassDefinitionResponseDTO>>> getClassDefinitionsForCourse(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "Whether to include only active class definitions")
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        log.debug("REST request to get classes for course: {} (activeOnly: {})", courseUuid, activeOnly);
        
        List<ClassDefinitionResponseDTO> result = activeOnly 
            ? classDefinitionService.findActiveClassesForCourse(courseUuid)
            : classDefinitionService.findClassesForCourse(courseUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definitions for course retrieved successfully"));
    }

    @Operation(summary = "Get class definitions for a training program")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definitions retrieved successfully")
    @GetMapping("/program/{programUuid}")
    public ResponseEntity<ApiResponse<List<ClassDefinitionResponseDTO>>> getClassDefinitionsForProgram(
            @Parameter(description = "UUID of the training program", required = true)
            @PathVariable UUID programUuid,
            @Parameter(description = "Whether to include only active class definitions")
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        log.debug("REST request to get classes for training program: {} (activeOnly: {})", programUuid, activeOnly);

        List<ClassDefinitionResponseDTO> result = activeOnly
                ? classDefinitionService.findActiveClassesForProgram(programUuid)
                : classDefinitionService.findClassesForProgram(programUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definitions for training program retrieved successfully"));
    }

    @Operation(summary = "Get class definitions for an instructor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definitions retrieved successfully")
    @GetMapping("/instructor/{instructorUuid}")
    public ResponseEntity<ApiResponse<List<ClassDefinitionResponseDTO>>> getClassDefinitionsForInstructor(
            @Parameter(description = "UUID of the instructor", required = true)
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Whether to include only active class definitions")
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        log.debug("REST request to get classes for instructor: {} (activeOnly: {})", instructorUuid, activeOnly);
        
        List<ClassDefinitionResponseDTO> result = activeOnly 
            ? classDefinitionService.findActiveClassesForInstructor(instructorUuid)
            : classDefinitionService.findClassesForInstructor(instructorUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definitions for instructor retrieved successfully"));
    }

    @Operation(summary = "Get class definitions for an organisation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definitions retrieved successfully")
    @GetMapping("/organisation/{organisationUuid}")
    public ResponseEntity<ApiResponse<List<ClassDefinitionResponseDTO>>> getClassDefinitionsForOrganisation(
            @Parameter(description = "UUID of the organisation", required = true)
            @PathVariable UUID organisationUuid) {
        log.debug("REST request to get classes for organisation: {}", organisationUuid);
        
        List<ClassDefinitionResponseDTO> result = classDefinitionService.findClassesForOrganisation(organisationUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definitions for organisation retrieved successfully"));
    }

    @Operation(summary = "Get all class definitions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definitions retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedDTO<ClassDefinitionResponseDTO>>> getAllClassDefinitions(
            Pageable pageable) {
        log.debug("REST request to get all classes (page: {}, size: {})", pageable.getPageNumber(), pageable.getPageSize());

        Page<ClassDefinitionResponseDTO> result = classDefinitionService.findAllClasses(pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(result, baseUrl),
                "All class definitions retrieved successfully"));
    }

    @Operation(summary = "Get all active class definitions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active class definitions retrieved successfully")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ClassDefinitionResponseDTO>>> getAllActiveClassDefinitions() {
        log.debug("REST request to get all active classes");
        
        List<ClassDefinitionResponseDTO> result = classDefinitionService.findAllActiveClasses();
        return ResponseEntity.ok(ApiResponse.success(result, "All active class definitions retrieved successfully"));
    }

    @Operation(summary = "Get class schedule")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class schedule retrieved successfully")
    @GetMapping("/{uuid}/schedule")
    public ResponseEntity<ApiResponse<PagedDTO<ScheduledInstanceDTO>>> getClassSchedule(
            @Parameter(description = "UUID of the class definition", required = true)
            @PathVariable UUID uuid,
            Pageable pageable) {
        log.debug("REST request to get schedule for class definition: {}", uuid);

        Page<ScheduledInstanceDTO> schedule = classDefinitionService.getClassSchedule(uuid, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(schedule, baseUrl),
                "Class schedule retrieved successfully"));
    }

    @Operation(summary = "Get class scheduling conflicts")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class scheduling conflicts retrieved successfully")
    @GetMapping("/{uuid}/scheduling-conflicts")
    public ResponseEntity<ApiResponse<PagedDTO<ClassSchedulingConflictDTO>>> getClassSchedulingConflicts(
            @Parameter(description = "UUID of the class definition", required = true)
            @PathVariable UUID uuid,
            Pageable pageable) {
        log.debug("REST request to get scheduling conflicts for class definition: {}", uuid);

        Page<ClassSchedulingConflictDTO> conflicts = classDefinitionService.getSchedulingConflicts(uuid, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(conflicts, baseUrl),
                "Class scheduling conflicts retrieved successfully"));
    }

    private String resolveClassMediaPath(String filePath) {
        String normalizedFilePath = StoragePathUtils.normalizeRelativePath(filePath);
        if (normalizedFilePath == null || normalizedFilePath.contains("/")) {
            return normalizedFilePath;
        }
        if (storageService.isVideo(normalizedFilePath)) {
            return storageProperties.getFolders().getClassPromotionalVideos() + "/" + normalizedFilePath;
        }
        return storageProperties.getFolders().getClassThumbnails() + "/" + normalizedFilePath;
    }

    private ClassDefinitionCreateRequestDTO resolveMultipartCreateRequest(String requestPart,
                                                                          String classDefinitionPart,
                                                                          MultiValueMap<String, String> formFields) {
        String jsonPayload = firstText(
                requestPart,
                classDefinitionPart,
                formFields.getFirst("request"),
                formFields.getFirst("class_definition"),
                formFields.getFirst("classDefinition"),
                formFields.getFirst("payload")
        );

        ClassDefinitionCreateRequestDTO request = jsonPayload == null
                ? objectMapper.convertValue(toClassDefinitionFieldMap(formFields), ClassDefinitionCreateRequestDTO.class)
                : readCreateRequest(jsonPayload);

        Set<ConstraintViolation<ClassDefinitionCreateRequestDTO>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return request;
    }

    private ClassDefinitionCreateRequestDTO readCreateRequest(String jsonPayload) {
        try {
            return objectMapper.readValue(jsonPayload, ClassDefinitionCreateRequestDTO.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid class definition JSON payload: " + ex.getOriginalMessage(), ex);
        }
    }

    private Map<String, Object> toClassDefinitionFieldMap(MultiValueMap<String, String> formFields) {
        Map<String, Object> values = new LinkedHashMap<>();
        formFields.forEach((fieldName, fieldValues) -> {
            if (fieldValues == null || fieldValues.isEmpty() || isMultipartControlField(fieldName)) {
                return;
            }
            String fieldValue = firstText(fieldValues.toArray(String[]::new));
            if (fieldValue == null) {
                return;
            }

            String jsonFieldName = normalizeFormFieldName(fieldName);
            if ("session_templates".equals(jsonFieldName)) {
                values.put(jsonFieldName, readSessionTemplates(fieldValue));
            } else {
                values.put(jsonFieldName, fieldValue);
            }
        });
        return values;
    }

    private List<ClassSessionTemplateDTO> readSessionTemplates(String fieldValue) {
        try {
            return objectMapper.readValue(fieldValue, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid session_templates JSON payload: " + ex.getOriginalMessage(), ex);
        }
    }

    private boolean isMultipartControlField(String fieldName) {
        return "request".equals(fieldName)
                || "class_definition".equals(fieldName)
                || "classDefinition".equals(fieldName)
                || "payload".equals(fieldName)
                || "thumbnail".equals(fieldName)
                || "promotional_video".equals(fieldName)
                || "promotionalVideo".equals(fieldName)
                || "marketing_video".equals(fieldName)
                || "marketingVideo".equals(fieldName);
    }

    private String normalizeFormFieldName(String fieldName) {
        if (fieldName.contains("_")) {
            return fieldName;
        }
        return fieldName
                .replace("UUID", "Uuid")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
    }

    private MultipartFile fileOrNull(MultipartFile... files) {
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                return file;
            }
        }
        return null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

}
