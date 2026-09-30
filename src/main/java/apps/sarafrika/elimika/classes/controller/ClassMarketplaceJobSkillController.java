package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequiredSkillsDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequiredSkillsRequest;
import apps.sarafrika.elimika.classes.service.ClassMarketplaceJobRequiredSkillService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Required skills of a marketplace job: optional tags set by the posting organisation. */
@RestController
@RequestMapping(ClassMarketplaceJobController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Class Marketplace Jobs", description = "Marketplace class adverts posted by organisations, and the recruitment funnel that ends in a hire")
public class ClassMarketplaceJobSkillController {

    private final ClassMarketplaceJobRequiredSkillService requiredSkillService;

    @Operation(operationId = "getMarketplaceJobRequiredSkills", summary = "Get a marketplace job's required skills",
            description = "Managers of the posting organisation and platform admins (403 otherwise). A job with no tags "
                    + "of its own returns its course's skills with inherited=true (all mandatory, the course's level as "
                    + "min_proficiency).")
    @GetMapping("/{jobUuid}/required-skills")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobRequiredSkillsDTO>> getRequiredSkills(@PathVariable UUID jobUuid) {
        return ResponseEntity.ok(ApiResponse.success(requiredSkillService.getRequiredSkills(jobUuid),
                "Required skills retrieved successfully"));
    }

    @Operation(operationId = "replaceMarketplaceJobRequiredSkills", summary = "Replace a marketplace job's required skills",
            description = "Managers of the posting organisation and platform admins, the same rule as editing the job. "
                    + "The body is the complete list; [] clears it and the job inherits its course's skills again. "
                    + "Skills come from GET /api/v1/skills: an unknown skill, a duplicate or a newly added retired skill is a 400. "
                    + "Tags are optional and never block publishing.")
    @PutMapping("/{jobUuid}/required-skills")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobRequiredSkillsDTO>> replaceRequiredSkills(
            @PathVariable UUID jobUuid,
            @Valid @RequestBody ClassMarketplaceJobRequiredSkillsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(requiredSkillService.replaceRequiredSkills(jobUuid, request),
                "Required skills updated successfully"));
    }
}
