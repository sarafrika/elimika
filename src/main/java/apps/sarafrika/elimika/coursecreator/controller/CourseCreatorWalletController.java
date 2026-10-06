package apps.sarafrika.elimika.coursecreator.controller;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorAchievementDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCompetencyDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorPortfolioItemDTO;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorWalletService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Portfolio, competency and achievement tabs of a course creator's skills wallet; owner or platform admin. */
@RestController
@RequestMapping(CourseCreatorController.API_ROOT_PATH + "/{courseCreatorUuid}")
@RequiredArgsConstructor
@Tag(name = "Course Creator Skills Wallet", description = "Portfolio, competencies and achievements a platform admin reviews")
@PreAuthorize("@domainSecurityService.isCourseCreatorWithUuid(#courseCreatorUuid) or @domainSecurityService.isPlatformAdmin()")
public class CourseCreatorWalletController {

    private final CourseCreatorWalletService walletService;

    @Operation(operationId = "getCourseCreatorPortfolio", summary = "List portfolio items")
    @GetMapping("/portfolio")
    public ResponseEntity<ApiResponse<List<CourseCreatorPortfolioItemDTO>>> listPortfolio(@PathVariable UUID courseCreatorUuid) {
        return ResponseEntity.ok(ApiResponse.success(walletService.listPortfolio(courseCreatorUuid), "Portfolio retrieved"));
    }

    @Operation(operationId = "addCourseCreatorPortfolioItem", summary = "Add a portfolio item")
    @PostMapping("/portfolio")
    public ResponseEntity<ApiResponse<CourseCreatorPortfolioItemDTO>> addPortfolioItem(
            @PathVariable UUID courseCreatorUuid, @Valid @RequestBody CourseCreatorPortfolioItemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                walletService.savePortfolioItem(courseCreatorUuid, null, dto), "Portfolio item added"));
    }

    @Operation(operationId = "updateCourseCreatorPortfolioItem", summary = "Update a portfolio item")
    @PutMapping("/portfolio/{itemUuid}")
    public ResponseEntity<ApiResponse<CourseCreatorPortfolioItemDTO>> updatePortfolioItem(
            @PathVariable UUID courseCreatorUuid, @PathVariable UUID itemUuid,
            @Valid @RequestBody CourseCreatorPortfolioItemDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                walletService.savePortfolioItem(courseCreatorUuid, itemUuid, dto), "Portfolio item updated"));
    }

    @Operation(operationId = "deleteCourseCreatorPortfolioItem", summary = "Delete a portfolio item")
    @DeleteMapping("/portfolio/{itemUuid}")
    public ResponseEntity<Void> deletePortfolioItem(@PathVariable UUID courseCreatorUuid, @PathVariable UUID itemUuid) {
        walletService.deletePortfolioItem(courseCreatorUuid, itemUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "getCourseCreatorCompetencies", summary = "List competencies")
    @GetMapping("/competencies")
    public ResponseEntity<ApiResponse<List<CourseCreatorCompetencyDTO>>> listCompetencies(@PathVariable UUID courseCreatorUuid) {
        return ResponseEntity.ok(ApiResponse.success(walletService.listCompetencies(courseCreatorUuid), "Competencies retrieved"));
    }

    @Operation(operationId = "addCourseCreatorCompetency", summary = "Add a competency")
    @PostMapping("/competencies")
    public ResponseEntity<ApiResponse<CourseCreatorCompetencyDTO>> addCompetency(
            @PathVariable UUID courseCreatorUuid, @Valid @RequestBody CourseCreatorCompetencyDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                walletService.saveCompetency(courseCreatorUuid, null, dto), "Competency added"));
    }

    @Operation(operationId = "updateCourseCreatorCompetency", summary = "Update a competency",
            description = "Changing the evidence sends the competency back for verification.")
    @PutMapping("/competencies/{itemUuid}")
    public ResponseEntity<ApiResponse<CourseCreatorCompetencyDTO>> updateCompetency(
            @PathVariable UUID courseCreatorUuid, @PathVariable UUID itemUuid,
            @Valid @RequestBody CourseCreatorCompetencyDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                walletService.saveCompetency(courseCreatorUuid, itemUuid, dto), "Competency updated"));
    }

    @Operation(operationId = "deleteCourseCreatorCompetency", summary = "Delete a competency")
    @DeleteMapping("/competencies/{itemUuid}")
    public ResponseEntity<Void> deleteCompetency(@PathVariable UUID courseCreatorUuid, @PathVariable UUID itemUuid) {
        walletService.deleteCompetency(courseCreatorUuid, itemUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "getCourseCreatorAchievements", summary = "List achievements")
    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<List<CourseCreatorAchievementDTO>>> listAchievements(@PathVariable UUID courseCreatorUuid) {
        return ResponseEntity.ok(ApiResponse.success(walletService.listAchievements(courseCreatorUuid), "Achievements retrieved"));
    }

    @Operation(operationId = "addCourseCreatorAchievement", summary = "Add an achievement")
    @PostMapping("/achievements")
    public ResponseEntity<ApiResponse<CourseCreatorAchievementDTO>> addAchievement(
            @PathVariable UUID courseCreatorUuid, @Valid @RequestBody CourseCreatorAchievementDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                walletService.saveAchievement(courseCreatorUuid, null, dto), "Achievement added"));
    }

    @Operation(operationId = "updateCourseCreatorAchievement", summary = "Update an achievement")
    @PutMapping("/achievements/{itemUuid}")
    public ResponseEntity<ApiResponse<CourseCreatorAchievementDTO>> updateAchievement(
            @PathVariable UUID courseCreatorUuid, @PathVariable UUID itemUuid,
            @Valid @RequestBody CourseCreatorAchievementDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                walletService.saveAchievement(courseCreatorUuid, itemUuid, dto), "Achievement updated"));
    }

    @Operation(operationId = "deleteCourseCreatorAchievement", summary = "Delete an achievement")
    @DeleteMapping("/achievements/{itemUuid}")
    public ResponseEntity<Void> deleteAchievement(@PathVariable UUID courseCreatorUuid, @PathVariable UUID itemUuid) {
        walletService.deleteAchievement(courseCreatorUuid, itemUuid);
        return ResponseEntity.noContent().build();
    }
}
