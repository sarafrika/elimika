package apps.sarafrika.elimika.skills.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.skills.dto.SkillDTO;
import apps.sarafrika.elimika.skills.internal.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The skill picker used when tagging courses and jobs. */
@RestController
@RequestMapping(SkillController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Skills", description = "The admin-curated skills taxonomy used to tag courses and jobs")
public class SkillController {

    public static final String API_ROOT_PATH = "/api/v1/skills";

    private final SkillService skillService;

    @Operation(operationId = "listSkills", summary = "List active skills",
            description = "Active skills only, for tag pickers. q matches names, slugs and aliases in memory over the "
                    + "small curated list: an exact match first, then names starting with q, then any containing it. "
                    + "Without q, skills are in name order. limit is 1-500 (default 50).")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillDTO>>> list(
            @Parameter(description = "Optional text; at most 200 characters")
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(skillService.listActive(q, limit), "Skills retrieved successfully"));
    }
}
