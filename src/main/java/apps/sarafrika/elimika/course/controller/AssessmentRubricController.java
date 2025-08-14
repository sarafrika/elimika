package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.AssessmentRubricDTO;
import apps.sarafrika.elimika.course.dto.RubricCriteriaDTO;
import apps.sarafrika.elimika.course.dto.RubricScoringDTO;
import apps.sarafrika.elimika.course.service.AssessmentRubricService;
import apps.sarafrika.elimika.course.service.RubricCriteriaService;
import apps.sarafrika.elimika.course.service.RubricScoringService;
import apps.sarafrika.elimika.course.service.RubricMatrixService;
import apps.sarafrika.elimika.course.dto.RubricMatrixDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping(AssessmentRubricController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Rubric Management", description = "Comprehensive management of assessment rubrics, including their criteria, scoring levels, and matrix configurations.")
public class AssessmentRubricController {

    public static final String API_ROOT_PATH = "/api/v1/rubrics";

    private final AssessmentRubricService assessmentRubricService;
    private final RubricCriteriaService rubricCriteriaService;
    private final RubricScoringService rubricScoringService;
    private final RubricMatrixService rubricMatrixService;

    @Operation(summary = "Create a new assessment rubric", description = "Creates a new assessment rubric. The rubric can be associated with a specific course or be a general-purpose rubric.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AssessmentRubricDTO>> createAssessmentRubric(@Valid @RequestBody AssessmentRubricDTO assessmentRubricDTO) {
        AssessmentRubricDTO createdRubric = assessmentRubricService.createAssessmentRubric(assessmentRubricDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdRubric, "Assessment rubric created successfully"));
    }

    @Operation(summary = "Get an assessment rubric by UUID", description = "Retrieves a single assessment rubric by its unique identifier.")
    @GetMapping(value = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AssessmentRubricDTO>> getAssessmentRubricByUuid(@PathVariable UUID uuid) {
        AssessmentRubricDTO assessmentRubricDTO = assessmentRubricService.getAssessmentRubricByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(assessmentRubricDTO, "Assessment rubric retrieved successfully"));
    }

    @Operation(summary = "Get all assessment rubrics", description = "Retrieves a paginated list of all assessment rubrics.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> getAllAssessmentRubrics(Pageable pageable) {
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.getAllAssessmentRubrics(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(rubrics, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Assessment rubrics retrieved successfully"));
    }

    @Operation(summary = "Update an assessment rubric", description = "Updates an existing assessment rubric.")
    @PutMapping(value = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AssessmentRubricDTO>> updateAssessmentRubric(@PathVariable UUID uuid, @Valid @RequestBody AssessmentRubricDTO assessmentRubricDTO) {
        AssessmentRubricDTO updatedRubric = assessmentRubricService.updateAssessmentRubric(uuid, assessmentRubricDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedRubric, "Assessment rubric updated successfully"));
    }

    @Operation(summary = "Delete an assessment rubric", description = "Deletes an assessment rubric and all its associated criteria and scoring levels.")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteAssessmentRubric(@PathVariable UUID uuid) {
        assessmentRubricService.deleteAssessmentRubric(uuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search for assessment rubrics", description = "Searches for assessment rubrics based on a set of filter criteria.")
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> searchAssessmentRubrics(@RequestParam java.util.Map<String, String> searchParams, Pageable pageable) {
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.search(searchParams, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(rubrics, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Assessment rubrics search completed successfully"));
    }

    @Operation(summary = "Add a criterion to a rubric", description = "Adds a new criterion to an existing assessment rubric.")
    @PostMapping(value = "/{rubricUuid}/criteria", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricCriteriaDTO>> addRubricCriterion(@PathVariable UUID rubricUuid, @Valid @RequestBody RubricCriteriaDTO rubricCriteriaDTO) {
        RubricCriteriaDTO createdCriterion = rubricCriteriaService.createRubricCriteria(rubricUuid, rubricCriteriaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdCriterion, "Rubric criterion added successfully"));
    }

    @Operation(summary = "Get all criteria for a rubric", description = "Retrieves a paginated list of all criteria for a specific assessment rubric.")
    @GetMapping(value = "/{rubricUuid}/criteria", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<RubricCriteriaDTO>>> getRubricCriteria(@PathVariable UUID rubricUuid, Pageable pageable) {
        Page<RubricCriteriaDTO> criteria = rubricCriteriaService.getAllByRubricUuid(rubricUuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(criteria, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Rubric criteria retrieved successfully"));
    }

    @Operation(summary = "Update a rubric criterion", description = "Updates an existing criterion within a rubric.")
    @PutMapping(value = "/{rubricUuid}/criteria/{criteriaUuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricCriteriaDTO>> updateRubricCriterion(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid, @Valid @RequestBody RubricCriteriaDTO rubricCriteriaDTO) {
        RubricCriteriaDTO updatedCriterion = rubricCriteriaService.updateRubricCriteria(rubricUuid, criteriaUuid, rubricCriteriaDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedCriterion, "Rubric criterion updated successfully"));
    }

    @Operation(summary = "Delete a rubric criterion", description = "Deletes a criterion from a rubric.")
    @DeleteMapping("/{rubricUuid}/criteria/{criteriaUuid}")
    public ResponseEntity<Void> deleteRubricCriterion(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid) {
        rubricCriteriaService.deleteRubricCriteria(rubricUuid, criteriaUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add a scoring level to a criterion", description = "Adds a new scoring level to an existing rubric criterion.")
    @PostMapping(value = "/{rubricUuid}/criteria/{criteriaUuid}/scoring", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricScoringDTO>> addRubricScoring(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid, @Valid @RequestBody RubricScoringDTO rubricScoringDTO) {
        RubricScoringDTO createdScoring = rubricScoringService.createRubricScoring(criteriaUuid, rubricScoringDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdScoring, "Rubric scoring added successfully"));
    }

    @Operation(summary = "Get all scoring levels for a criterion", description = "Retrieves a paginated list of all scoring levels for a specific rubric criterion.")
    @GetMapping(value = "/{rubricUuid}/criteria/{criteriaUuid}/scoring", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<RubricScoringDTO>>> getRubricScoring(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid, Pageable pageable) {
        Page<RubricScoringDTO> scoring = rubricScoringService.getAllByCriteriaUuid(criteriaUuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(scoring, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Rubric scoring retrieved successfully"));
    }

    @Operation(summary = "Update a scoring level", description = "Updates an existing scoring level for a criterion.")
    @PutMapping(value = "/{rubricUuid}/criteria/{criteriaUuid}/scoring/{scoringUuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricScoringDTO>> updateRubricScoring(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid, @PathVariable UUID scoringUuid, @Valid @RequestBody RubricScoringDTO rubricScoringDTO) {
        RubricScoringDTO updatedScoring = rubricScoringService.updateRubricScoring(criteriaUuid, scoringUuid, rubricScoringDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedScoring, "Rubric scoring updated successfully"));
    }

    @Operation(summary = "Delete a scoring level", description = "Deletes a scoring level from a criterion.")
    @DeleteMapping("/{rubricUuid}/criteria/{criteriaUuid}/scoring/{scoringUuid}")
    public ResponseEntity<Void> deleteRubricScoring(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid, @PathVariable UUID scoringUuid) {
        rubricScoringService.deleteRubricScoring(criteriaUuid, scoringUuid);
        return ResponseEntity.noContent().build();
    }

    // Matrix-based endpoints
    @Operation(summary = "Get rubric matrix view", description = "Retrieves the complete rubric matrix with all criteria, scoring levels, and cell intersections.")
    @GetMapping(value = "/{rubricUuid}/matrix-view", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricMatrixDTO>> getRubricMatrixView(@PathVariable UUID rubricUuid) {
        RubricMatrixDTO matrix = rubricMatrixService.getRubricMatrix(rubricUuid);
        return ResponseEntity.ok(ApiResponse.success(matrix, "Rubric matrix retrieved successfully"));
    }

    @Operation(summary = "Initialize rubric matrix", description = "Sets up the rubric matrix with default scoring levels and prepares it for use.")
    @PostMapping(value = "/{rubricUuid}/initialize-matrix", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricMatrixDTO>> initializeMatrix(
            @PathVariable UUID rubricUuid,
            @RequestParam(defaultValue = "standard") String template,
            @RequestParam(defaultValue = "SYSTEM") String createdBy) {
        
        RubricMatrixDTO matrix = rubricMatrixService.initializeRubricMatrix(rubricUuid, template, createdBy);
        return ResponseEntity.ok(ApiResponse.success(matrix, "Rubric matrix initialized successfully"));
    }

    @Operation(summary = "Validate rubric matrix", description = "Validates the matrix for completeness and consistency before use in assessments.")
    @GetMapping(value = "/{rubricUuid}/validate-matrix", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricMatrixService.MatrixValidationResult>> validateMatrix(@PathVariable UUID rubricUuid) {
        RubricMatrixService.MatrixValidationResult validation = rubricMatrixService.validateMatrix(rubricUuid);
        
        String message = validation.isValid() ? 
                "Rubric matrix is valid and ready for use" : 
                "Rubric matrix requires attention: " + validation.message();
        
        return ResponseEntity.ok(ApiResponse.success(validation, message));
    }
}
