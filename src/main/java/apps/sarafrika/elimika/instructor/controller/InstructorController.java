package apps.sarafrika.elimika.instructor.controller;

import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.instructor.dto.InstructorDTO;
import apps.sarafrika.elimika.instructor.service.InstructorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing instructor operations.
 */
@RestController @CrossOrigin
@RequestMapping(InstructorController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Instructor Management", description = "Endpoints for managing instructors")
public class InstructorController {

    public static final String API_ROOT_PATH = "/api/v1/instructors";

    private final InstructorService instructorService;

    /**
     * Retrieves an instructor by UUID.
     *
     * @param uuid The UUID of the instructor.
     * @return The instructor DTO if found.
     */
    @Operation(
            summary = "Get instructor by UUID",
            description = "Fetches an instructor by their UUID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Instructor found",
                            content = @Content(schema = @Schema(implementation = InstructorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Instructor not found")
            }
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<InstructorDTO>> getInstructorByUuid(@PathVariable UUID uuid) {
        InstructorDTO instructorDTO = instructorService.getInstructorByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(instructorDTO, "Instructor record fetched successfully"));
    }

    /**
     * Retrieves a paginated list of all instructors.
     *
     * @param pageable Pagination details.
     * @return A paginated list of instructor DTOs.
     */
    @Operation(
            summary = "Get all instructors",
            description = "Fetches a paginated list of instructors."
    )
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<InstructorDTO>>> getAllInstructors(Pageable pageable) {
        Page<InstructorDTO> instructors = instructorService.getAllInstructors(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(instructors, ServletUriComponentsBuilder
                        .fromCurrentRequestUri().build().toString()),
                        "Instructors fetched successfully"));
    }

    /**
     * Updates an existing instructor by UUID.
     *
     * @param uuid The UUID of the instructor to update.
     * @param instructorDTO The updated instructor data.
     * @return The updated instructor DTO.
     */
    @Operation(
            summary = "Update an instructor",
            description = "Updates an existing instructor record.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Instructor updated successfully",
                            content = @Content(schema = @Schema(implementation = InstructorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Instructor not found")
            }
    )
    @PutMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<InstructorDTO>> updateInstructor(
            @PathVariable UUID uuid,
            @Valid @RequestBody InstructorDTO instructorDTO) {
        InstructorDTO updatedInstructor = instructorService.updateInstructor(uuid, instructorDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(updatedInstructor, "Instructor updated successfully"));
    }

    /**
     * Deletes an instructor by UUID.
     *
     * @param uuid The UUID of the instructor to delete.
     * @return A response entity with no content.
     */
    @Operation(
            summary = "Delete an instructor",
            description = "Removes an instructor record from the system.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Instructor deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Instructor not found")
            }
    )
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteInstructor(@PathVariable UUID uuid) {
        instructorService.deleteInstructor(uuid);
        return ResponseEntity.noContent().build();
    }

    /**
     * Searches for instructors with pagination.
     *
     * @param searchParams Search parameters as key-value pairs.
     * @param pageable Pagination details.
     * @return A paginated list of matching instructor DTOs.
     */
    @Operation(
            summary = "Search instructors",
            description = "Search for instructors based on criteria.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search results returned successfully",
                            content = @Content(schema = @Schema(implementation = Page.class)))
            }
    )
    @GetMapping("/search")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<InstructorDTO>>> searchInstructors(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<InstructorDTO> instructors = instructorService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(instructors, ServletUriComponentsBuilder
                        .fromCurrentRequestUri().build().toString()),
                        "Instructor search successful"));
    }
}
