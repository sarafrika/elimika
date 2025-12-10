package apps.sarafrika.elimika.booking.controller;

import apps.sarafrika.elimika.booking.dto.BookingPaymentUpdateRequestDTO;
import apps.sarafrika.elimika.booking.dto.BookingResponseDTO;
import apps.sarafrika.elimika.booking.dto.CreateBookingRequestDTO;
import apps.sarafrika.elimika.booking.spi.BookingService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking", description = "APIs for booking instructors and managing payments")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Create a booking for a course/instructor slot")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponseDTO>> createBooking(
            @Valid @RequestBody CreateBookingRequestDTO request) {
        log.debug("Request to create booking for instructor {} and course {}", request.instructorUuid(), request.courseUuid());
        BookingResponseDTO booking = bookingService.createBooking(request);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking created"));
    }

    @Operation(summary = "Get booking details")
    @GetMapping("/{bookingUuid}")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> getBooking(
            @Parameter(description = "Booking UUID") @PathVariable UUID bookingUuid) {
        BookingResponseDTO booking = bookingService.getBooking(bookingUuid);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking retrieved"));
    }

    @Operation(summary = "Cancel a booking and release the reserved slot")
    @PostMapping("/{bookingUuid}/cancel")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> cancelBooking(
            @Parameter(description = "Booking UUID") @PathVariable UUID bookingUuid) {
        BookingResponseDTO booking = bookingService.cancelBooking(bookingUuid);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking cancelled"));
    }

    @Operation(summary = "Payment callback to update booking status")
    @PostMapping("/{bookingUuid}/payment-callback")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> paymentCallback(
            @Parameter(description = "Booking UUID") @PathVariable UUID bookingUuid,
            @Valid @RequestBody BookingPaymentUpdateRequestDTO request) {
        BookingResponseDTO booking = bookingService.applyPaymentUpdate(bookingUuid, request);
        return ResponseEntity.ok(ApiResponse.success(booking, "Payment status updated"));
    }
}
