package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.AssessmentRubricDTO;
import apps.sarafrika.elimika.course.dto.RubricCriteriaDTO;
import apps.sarafrika.elimika.course.dto.RubricScoringDTO;
import apps.sarafrika.elimika.course.service.AssessmentRubricService;
import apps.sarafrika.elimika.course.service.RubricCriteriaService;
import apps.sarafrika.elimika.course.service.RubricScoringService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping(AssessmentRubricController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Assessment Rubric Management", description = "Management of assessment rubrics, including their criteria and scoring levels.")
public class AssessmentRubricController {

    public static final String API_ROOT_PATH = "/api/v1/rubrics";

    private final AssessmentRubricService assessmentRubricService;
    private final RubricCriteriaService rubricCriteriaService;
    private final RubricScoringService rubricScoringService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssessmentRubricDTO>> createAssessmentRubric(@Valid @RequestBody AssessmentRubricDTO assessmentRubricDTO) {
        AssessmentRubricDTO createdRubric = assessmentRubricService.createAssessmentRubric(assessmentRubricDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdRubric, "Assessment rubric created successfully"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AssessmentRubricDTO>> getAssessmentRubricByUuid(@PathVariable UUID uuid) {
        AssessmentRubricDTO assessmentRubricDTO = assessmentRubricService.getAssessmentRubricByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(assessmentRubricDTO, "Assessment rubric retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> getAllAssessmentRubrics(Pageable pageable) {
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.getAllAssessmentRubrics(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(rubrics, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Assessment rubrics retrieved successfully"));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AssessmentRubricDTO>> updateAssessmentRubric(@PathVariable UUID uuid, @Valid @RequestBody AssessmentRubricDTO assessmentRubricDTO) {
        AssessmentRubricDTO updatedRubric = assessmentRubricService.updateAssessmentRubric(uuid, assessmentRubricDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedRubric, "Assessment rubric updated successfully"));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteAssessmentRubric(@PathVariable UUID uuid) {
        assessmentRubricService.deleteAssessmentRubric(uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> searchAssessmentRubrics(@RequestParam java.util.Map<String, String> searchParams, Pageable pageable) {
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.search(searchParams, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(rubrics, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Assessment rubrics search completed successfully"));
    }

    @PostMapping("/{rubricUuid}/criteria")
    public ResponseEntity<ApiResponse<RubricCriteriaDTO>> addRubricCriterion(@PathVariable UUID rubricUuid, @Valid @RequestBody RubricCriteriaDTO rubricCriteriaDTO) {
        RubricCriteriaDTO newRubricCriteriaDTO = new RubricCriteriaDTO(
                null,
                rubricUuid,
                rubricCriteriaDTO.componentName(),
                rubricCriteriaDTO.description(),
                rubricCriteriaDTO.displayOrder(),
                null,
                null,
                null,
                null
        );
        RubricCriteriaDTO createdCriterion = rubricCriteriaService.createRubricCriteria(newRubricCriteriaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdCriterion, "Rubric criterion added successfully"));
    }

    @GetMapping("/{rubricUuid}/criteria")
    public ResponseEntity<ApiResponse<PagedDTO<RubricCriteriaDTO>>> getRubricCriteria(@PathVariable UUID rubricUuid, Pageable pageable) {
        Page<RubricCriteriaDTO> criteria = rubricCriteriaService.getAllByRubricUuid(rubricUuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(criteria, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Rubric criteria retrieved successfully"));
    }

    @PutMapping("/{rubricUuid}/criteria/{criteriaUuid}")
    public ResponseEntity<ApiResponse<RubricCriteriaDTO>> updateRubricCriterion(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid, @Valid @RequestBody RubricCriteriaDTO rubricCriteriaDTO) {
        RubricCriteriaDTO updatedCriterion = rubricCriteriaService.updateRubricCriteria(criteriaUuid, rubricCriteriaDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedCriterion, "Rubric criterion updated successfully"));
    }

    @DeleteMapping("/{rubricUuid}/criteria/{criteriaUuid}")
    public ResponseEntity<Void> deleteRubricCriterion(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid) {
        rubricCriteriaService.deleteRubricCriteria(criteriaUuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{rubricUuid}/criteria/{criteriaUuid}/scoring")
    public ResponseEntity<ApiResponse<RubricScoringDTO>> addRubricScoring(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid, @Valid @RequestBody RubricScoringDTO rubricScoringDTO) {
        RubricScoringDTO newRubricScoringDTO = new RubricScoringDTO(
                null,
                criteriaUuid,
                rubricScoringDTO.gradingLevelUuid(),
                rubricScoringDTO.description(),
                null,
                null,
                null,
                null
        );
        RubricScoringDTO createdScoring = rubricScoringService.createRubricScoring(newRubricScoringDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdScoring, "Rubric scoring added successfully"));
    }

    @GetMapping("/{rubricUuid}/criteria/{criteriaUuid}/scoring")
    public ResponseEntity<ApiResponse<PagedDTO<RubricScoringDTO>>> getRubricScoring(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid, Pageable pageable) {
        Page<RubricScoringDTO> scoring = rubricScoringService.getAllByCriteriaUuid(criteriaUuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(scoring, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Rubric scoring retrieved successfully"));
    }

    @PutMapping("/{rubricUuid}/criteria/{criteriaUuid}/scoring/{scoringUuid}")
    public ResponseEntity<ApiResponse<RubricScoringDTO>> updateRubricScoring(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid, @PathVariable UUID scoringUuid, @Valid @RequestBody RubricScoringDTO rubricScoringDTO) {
        RubricScoringDTO updatedScoring = rubricScoringService.updateRubricScoring(scoringUuid, rubricScoringDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedScoring, "Rubric scoring updated successfully"));
    }

    @DeleteMapping("/{rubricUuid}/criteria/{criteriaUuid}/scoring/{scoringUuid}")
    public ResponseEntity<Void> deleteRubricScoring(@PathVariable UUID rubricUuid, @PathVariable UUID criteriaUuid, @PathVariable UUID scoringUuid) {
        rubricScoringService.deleteRubricScoring(scoringUuid);
        return ResponseEntity.noContent().build();
    }
}
