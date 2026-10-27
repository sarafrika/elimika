package apps.sarafrika.elimika.skills.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.skills.dto.SkillLevelDTO;
import apps.sarafrika.elimika.skills.internal.SkillLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The skill level scale, readable by anyone signed in so reports can label levels. */
@RestController
@RequestMapping(SkillLevelController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Skill Levels", description = "The platform-wide scale that turns skill evidence into a level")
public class SkillLevelController {

    public static final String API_ROOT_PATH = "/api/v1/skill-levels";

    private final SkillLevelService skillLevelService;

    @Operation(operationId = "listSkillLevels", summary = "List skill levels", description = "Lowest minimum first.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillLevelDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(skillLevelService.list(), "Skill levels retrieved successfully"));
    }
}
