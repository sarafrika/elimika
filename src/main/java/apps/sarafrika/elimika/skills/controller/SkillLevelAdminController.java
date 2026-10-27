package apps.sarafrika.elimika.skills.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.skills.dto.SkillLevelDTO;
import apps.sarafrika.elimika.skills.internal.SkillLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** System-admin editing of the skill level scale. Method-level guards would replace this class guard. */
@RestController
@RequestMapping(SkillLevelAdminController.API_ROOT_PATH)
@RequiredArgsConstructor
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
@Tag(name = "Skill Levels Admin", description = "Editing the platform-wide skill level scale")
public class SkillLevelAdminController {

    public static final String API_ROOT_PATH = "/api/v1/admin/skill-levels";

    private final SkillLevelService skillLevelService;

    @Operation(operationId = "replaceSkillLevels", summary = "Replace the skill level scale",
            description = "Saves the whole scale at once. One level must start at 0%, minimums and names must be unique, "
                    + "and achieved levels must be the top levels. Current learner levels are re-levelled.")
    @PutMapping
    public ResponseEntity<ApiResponse<List<SkillLevelDTO>>> replace(@Valid @RequestBody List<@Valid SkillLevelDTO> levels) {
        return ResponseEntity.ok(ApiResponse.success(skillLevelService.replace(levels), "Skill levels saved"));
    }
}
