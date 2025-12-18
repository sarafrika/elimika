package apps.sarafrika.elimika.booking.controller;

import apps.sarafrika.elimika.booking.dto.BookingResponseDTO;
import apps.sarafrika.elimika.booking.spi.BookingService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/instructors/{instructorUuid}/bookings")
@RequiredArgsConstructor
@Tag(name = "Instructor Bookings", description = "Endpoints for retrieving instructor bookings")
public class InstructorBookingController {

    private final BookingService bookingService;

    @Operation(summary = "Get instructor bookings")
    @GetMapping
    @PreAuthorize("@domainSecurityService.isInstructorWithUuid(#instructorUuid) or @domainSecurityService.isOrganizationAdmin()")
    public ResponseEntity<ApiResponse<PagedDTO<BookingResponseDTO>>> getInstructorBookings(
            @Parameter(description = "Instructor UUID") @PathVariable UUID instructorUuid,
            @Parameter(description = "Optional booking status filter (e.g., payment_required)")
            @RequestParam(value = "status", required = false) String status,
            Pageable pageable) {
        BookingStatus bookingStatus = parseStatus(status);
        Page<BookingResponseDTO> bookings = bookingService.getBookingsForInstructor(instructorUuid, bookingStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(bookings, ServletUriComponentsBuilder.fromCurrentRequest().build().toString()),
                "Instructor bookings retrieved successfully"));
    }

    private BookingStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return BookingStatus.fromValue(status.trim());
    }
}
