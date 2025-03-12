package apps.sarafrika.elimika.training.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.training.dto.TrainingSessionDTO;
import apps.sarafrika.elimika.training.service.TrainingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(TrainingSessionController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Training Session Management", description = "Endpoints for managing training sessions")
public class TrainingSessionController {
    public static final String API_ROOT_PATH = "/api/v1/training-sessions";

    private final TrainingSessionService trainingSessionService;

    /**
     * Creates a new training session.
     *
     * @param trainingSessionDTO The training session data to be created.
     * @return The created training session DTO.
     */
    @Operation(summary = "Create a new training session", description = "Saves a new training session record in the system.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Training session created successfully",
                            content = @Content(schema = @Schema(implementation = TrainingSessionDTO.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data")
            })
    @PostMapping
    public ResponseEntity<ApiResponse<TrainingSessionDTO>> createTrainingSession(@Valid @RequestBody TrainingSessionDTO trainingSessionDTO) {
        TrainingSessionDTO createdSession = trainingSessionService.createTrainingSession(trainingSessionDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdSession, "Training session created successfully"));
    }

    /**
     * Retrieves a training session by UUID.
     *
     * @param uuid The UUID of the training session.
     * @return The training session DTO if found.
     */
    @Operation(summary = "Get training session by ID", description = "Fetches a training session by its UUID.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training session found",
                            content = @Content(schema = @Schema(implementation = TrainingSessionDTO.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training session not found")
            })
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<TrainingSessionDTO>> getTrainingSessionById(@PathVariable UUID uuid) {
        TrainingSessionDTO trainingSessionDTO = trainingSessionService.getTrainingSessionByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(trainingSessionDTO, "Training session retrieved successfully"));
    }

    /**
     * Retrieves a paginated list of all training sessions.
     *
     * @param pageable Pagination details.
     * @return A paginated list of training session DTOs.
     */
    @Operation(summary = "Get all training sessions", description = "Fetches a paginated list of training sessions.")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedDTO<TrainingSessionDTO>>> getAllTrainingSessions(Pageable pageable) {
        Page<TrainingSessionDTO> sessions = trainingSessionService.getAllTrainingSessions(pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(sessions, ServletUriComponentsBuilder.fromCurrentRequest().build().toString()),
                "Training sessions retrieved successfully"));
    }

    /**
     * Updates an existing training session by UUID.
     *
     * @param uuid              The UUID of the training session to update.
     * @param trainingSessionDTO The updated training session data.
     * @return The updated training session DTO.
     */
    @Operation(summary = "Update a training session", description = "Updates an existing training session record.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training session updated successfully",
                            content = @Content(schema = @Schema(implementation = TrainingSessionDTO.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training session not found")
            })
    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<TrainingSessionDTO>> updateTrainingSession(
            @PathVariable UUID uuid, @Valid @RequestBody TrainingSessionDTO trainingSessionDTO) {
        TrainingSessionDTO updatedSession = trainingSessionService.updateTrainingSession(uuid, trainingSessionDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedSession, "Training session updated successfully"));
    }

    /**
     * Deletes a training session by UUID.
     *
     * @param uuid The UUID of the training session to delete.
     * @return A response entity with no content.
     */
    @Operation(summary = "Delete a training session", description = "Removes a training session record from the system.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Training session deleted successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training session not found")
            })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteTrainingSession(@PathVariable UUID uuid) {
        trainingSessionService.deleteTrainingSession(uuid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, "Training session deleted successfully"));
    }

    /**
     * Searches for training sessions with pagination.
     *
     * @param searchParams Search parameters as key-value pairs.
     * @param pageable     Pagination details.
     * @return A paginated list of matching training session DTOs.
     */
    @Operation(summary = "Search training sessions", description = "Search for training sessions based on criteria.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results returned successfully",
                            content = @Content(schema = @Schema(implementation = Page.class)))
            })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedDTO<TrainingSessionDTO>>> searchTrainingSessions(
            @RequestParam Map<String, String> searchParams, Pageable pageable) {
        Page<TrainingSessionDTO> sessions = trainingSessionService.searchTrainingSessions(searchParams, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(sessions, ServletUriComponentsBuilder.fromCurrentRequest().build().toString()),
                "Search successful"));
    }
}
