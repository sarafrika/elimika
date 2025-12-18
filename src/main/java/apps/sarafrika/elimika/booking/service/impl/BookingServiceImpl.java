package apps.sarafrika.elimika.booking.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.booking.dto.BookingPaymentRequestDTO;
import apps.sarafrika.elimika.booking.dto.BookingPaymentSessionDTO;
import apps.sarafrika.elimika.booking.dto.BookingPaymentUpdateRequestDTO;
import apps.sarafrika.elimika.booking.dto.BookingResponseDTO;
import apps.sarafrika.elimika.booking.dto.CreateBookingRequestDTO;
import apps.sarafrika.elimika.booking.model.Booking;
import apps.sarafrika.elimika.booking.payment.PaymentGatewayClient;
import apps.sarafrika.elimika.booking.payment.PaymentSession;
import apps.sarafrika.elimika.booking.repository.BookingRepository;
import apps.sarafrika.elimika.booking.spi.BookingService;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final int DEFAULT_HOLD_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final PaymentGatewayClient paymentGatewayClient;

    @Override
    public BookingResponseDTO createBooking(CreateBookingRequestDTO request) {
        validateCreateRequest(request);

        if (!availabilityService.isInstructorAvailable(request.instructorUuid(), request.startTime(), request.endTime())) {
            throw new IllegalStateException("Instructor is not available for the requested time range.");
        }

        Booking booking = new Booking();
        booking.setStudentUuid(request.studentUuid());
        booking.setCourseUuid(request.courseUuid());
        booking.setInstructorUuid(request.instructorUuid());
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(BookingStatus.PAYMENT_REQUIRED);
        booking.setHoldExpiresAt(resolveHoldExpiry(request.startTime()));
        booking.setPriceAmount(resolvePrice(request.priceAmount()));
        booking.setCurrency(request.currency());
        booking.setPurpose(request.purpose());

        Booking saved = bookingRepository.save(booking);

        PaymentSession session = paymentGatewayClient.initiatePayment(saved);
        saved.setPaymentSessionId(session.sessionId());
        saved.setPaymentEngine(session.engine());
        Booking updated = bookingRepository.save(saved);

        log.debug("Created booking {} for student {} course {} instructor {}", updated.getUuid(),
                updated.getStudentUuid(), updated.getCourseUuid(), updated.getInstructorUuid());
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDTO getBooking(UUID bookingUuid) {
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingUuid));
        return mapToResponse(booking);
    }

    @Override
    public BookingResponseDTO cancelBooking(UUID bookingUuid) {
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingUuid));

        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            return mapToResponse(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setAvailabilityBlockUuid(null);
        Booking saved = bookingRepository.save(booking);
        return mapToResponse(saved);
    }

    @Override
    public BookingResponseDTO applyPaymentUpdate(UUID bookingUuid, BookingPaymentUpdateRequestDTO request) {
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingUuid));

        if ("succeeded".equalsIgnoreCase(request.paymentStatus())) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentReference(request.paymentReference());
            booking.setPaymentEngine(request.paymentEngine());
        } else {
            booking.setStatus(BookingStatus.PAYMENT_FAILED);
            booking.setPaymentReference(request.paymentReference());
            booking.setPaymentEngine(request.paymentEngine());
            booking.setAvailabilityBlockUuid(null);
        }

        Booking saved = bookingRepository.save(booking);
        return mapToResponse(saved);
    }

    @Override
    public BookingPaymentSessionDTO requestPayment(UUID bookingUuid, BookingPaymentRequestDTO request) {
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingUuid));

        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new IllegalStateException("Cannot request payment for a cancelled booking");
        }
        if (BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new IllegalStateException("Booking is already paid");
        }

        if (request != null && request.paymentEngine() != null && !request.paymentEngine().isBlank()) {
            booking.setPaymentEngine(request.paymentEngine().trim());
        }

        booking.setStatus(BookingStatus.PAYMENT_REQUIRED);
        booking.setHoldExpiresAt(resolveHoldExpiry(booking.getStartTime()));

        Booking saved = bookingRepository.save(booking);
        PaymentSession session = paymentGatewayClient.initiatePayment(saved);
        saved.setPaymentSessionId(session.sessionId());
        saved.setPaymentEngine(session.engine());
        Booking updated = bookingRepository.save(saved);

        return new BookingPaymentSessionDTO(
                updated.getUuid(),
                session.sessionId(),
                session.paymentUrl(),
                session.engine(),
                updated.getHoldExpiresAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getBookingsForStudent(UUID studentUuid, BookingStatus status, Pageable pageable) {
        if (studentUuid == null) {
            throw new IllegalArgumentException("Student UUID cannot be null");
        }

        Page<Booking> bookings = status == null
                ? bookingRepository.findByStudentUuid(studentUuid, pageable)
                : bookingRepository.findByStudentUuidAndStatus(studentUuid, status, pageable);

        return bookings.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getBookingsForInstructor(UUID instructorUuid, BookingStatus status, Pageable pageable) {
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }

        Page<Booking> bookings = status == null
                ? bookingRepository.findByInstructorUuid(instructorUuid, pageable)
                : bookingRepository.findByInstructorUuidAndStatus(instructorUuid, status, pageable);

        return bookings.map(this::mapToResponse);
    }

    @Override
    public void expireHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expired = bookingRepository.findByStatusAndHoldExpiresAtBefore(BookingStatus.PAYMENT_REQUIRED, now);
        expired.forEach(booking -> {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setAvailabilityBlockUuid(null);
        });
        bookingRepository.saveAll(expired);
        if (!expired.isEmpty()) {
            log.info("Expired {} booking holds", expired.size());
        }
    }

    private void validateCreateRequest(CreateBookingRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Booking request cannot be null");
        }
        if (request.studentUuid() == null) {
            throw new IllegalArgumentException("Student UUID cannot be null");
        }
        if (request.courseUuid() == null) {
            throw new IllegalArgumentException("Course UUID cannot be null");
        }
        if (request.instructorUuid() == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (request.startTime() == null || request.endTime() == null) {
            throw new IllegalArgumentException("Start and end time are required");
        }
        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    private LocalDateTime resolveHoldExpiry(LocalDateTime startTime) {
        LocalDateTime defaultExpiry = LocalDateTime.now().plusMinutes(DEFAULT_HOLD_MINUTES);
        if (startTime == null) {
            return defaultExpiry;
        }
        return startTime.isBefore(defaultExpiry) ? startTime : defaultExpiry;
    }

    private BigDecimal resolvePrice(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BookingResponseDTO mapToResponse(Booking booking) {
        return new BookingResponseDTO(
                booking.getUuid(),
                booking.getStudentUuid(),
                booking.getCourseUuid(),
                booking.getInstructorUuid(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus(),
                booking.getPriceAmount(),
                booking.getCurrency(),
                booking.getPaymentSessionId(),
                booking.getPaymentReference(),
                booking.getPaymentEngine(),
                booking.getHoldExpiresAt(),
                booking.getAvailabilityBlockUuid(),
                booking.getPurpose(),
                booking.getCreatedDate(),
                booking.getLastModifiedDate()
        );
    }
}
