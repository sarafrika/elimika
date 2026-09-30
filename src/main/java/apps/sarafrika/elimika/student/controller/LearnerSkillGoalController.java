package apps.sarafrika.elimika.student.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.student.dto.LearnerSkillGoalDTO;
import apps.sarafrika.elimika.student.dto.LearnerSkillGoalsUpdateRequest;
import apps.sarafrika.elimika.student.service.LearnerSkillGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** A learner's declared skill goals; course recommendations use them to find skill-gap courses. */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Learner Skill Goals", description = "Skills a learner wants to learn, from the skills taxonomy")
public class LearnerSkillGoalController {

    private final LearnerSkillGoalService learnerSkillGoalService;

    @Operation(operationId = "getLearnerSkillGoals", summary = "Get a learner's skill goals",
            description = "The learner, a platform admin, or a guardian whose share scope is FULL or ACADEMICS. Oldest first.")
    @PreAuthorize("@learnerSkillGoalSecurityService.canRead(#uuid)")
    @GetMapping("/{uuid}/skill-goals")
    public ResponseEntity<ApiResponse<List<LearnerSkillGoalDTO>>> getSkillGoals(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(learnerSkillGoalService.getGoals(uuid),
                "Skill goals retrieved successfully"));
    }

    @Operation(operationId = "replaceLearnerSkillGoals", summary = "Replace a learner's skill goals",
            description = "The learner only. The body is the complete list (at most 20); [] clears it. Skills come from "
                    + "GET /api/v1/skills: an unknown skill, a duplicate or a newly added retired skill is a 400.")
    @PreAuthorize("@learnerSkillGoalSecurityService.canWrite(#uuid)")
    @PutMapping("/{uuid}/skill-goals")
    public ResponseEntity<ApiResponse<List<LearnerSkillGoalDTO>>> replaceSkillGoals(
            @PathVariable UUID uuid,
            @Valid @RequestBody LearnerSkillGoalsUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(learnerSkillGoalService.replaceGoals(uuid, request.skillUuids()),
                "Skill goals updated successfully"));
    }
}
