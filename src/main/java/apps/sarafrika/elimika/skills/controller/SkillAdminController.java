package apps.sarafrika.elimika.skills.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.skills.dto.SkillDTO;
import apps.sarafrika.elimika.skills.dto.SkillRequest;
import apps.sarafrika.elimika.skills.internal.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Curation of the skills taxonomy. Platform admins only: the list is shared by every course, job
 * and instructor profile on the platform. A method-level {@code @PreAuthorize} would replace this
 * one, so any override must restate the platform-admin check.
 */
@RestController
@RequestMapping(SkillAdminController.API_ROOT_PATH)
@RequiredArgsConstructor
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
@Tag(name = "Skills Admin", description = "Curation of the admin-managed skills taxonomy")
public class SkillAdminController {

    public static final String API_ROOT_PATH = "/api/v1/admin/skills";

    private final SkillService skillService;

    @Operation(operationId = "adminListSkills", summary = "List every skill",
            description = "The whole taxonomy in name order, retired skills included unless active is given. "
                    + "q matches names, slugs and aliases in memory (no database text search).")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillDTO>>> list(
            @Parameter(description = "Optional text matched against name, slug and aliases")
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "active", required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success(skillService.listAll(q, active), "Skills retrieved successfully"));
    }

    @Operation(operationId = "adminGetSkill", summary = "Get a skill")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<SkillDTO>> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(skillService.get(uuid), "Skill retrieved successfully"));
    }

    @Operation(operationId = "adminCreateSkill", summary = "Create a skill",
            description = "409 when the slug, or an alias, already names another skill")
    @PostMapping
    public ResponseEntity<ApiResponse<SkillDTO>> create(@Valid @RequestBody SkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(skillService.create(request), "Skill created successfully"));
    }

    @Operation(operationId = "adminUpdateSkill", summary = "Replace a skill",
            description = "Every field is replaced; set active=false to retire a skill while keeping existing tags")
    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<SkillDTO>> update(@PathVariable UUID uuid, @Valid @RequestBody SkillRequest request) {
        return ResponseEntity.ok(ApiResponse.success(skillService.update(uuid, request), "Skill updated successfully"));
    }

    @Operation(operationId = "adminDeleteSkill", summary = "Delete a skill",
            description = "Removes it from every course and job tag list and unlinks instructor skills (their free text stays). "
                    + "Prefer retiring it with active=false.")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        skillService.delete(uuid);
        return ResponseEntity.noContent().build();
    }
}
