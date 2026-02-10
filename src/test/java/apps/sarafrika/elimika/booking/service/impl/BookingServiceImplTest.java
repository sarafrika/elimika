package apps.sarafrika.elimika.booking.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.booking.dto.BookingPaymentUpdateRequestDTO;
import apps.sarafrika.elimika.booking.dto.CreateBookingRequestDTO;
import apps.sarafrika.elimika.booking.model.Booking;
import apps.sarafrika.elimika.booking.payment.PaymentGatewayClient;
import apps.sarafrika.elimika.booking.payment.PaymentSession;
import apps.sarafrika.elimika.booking.repository.BookingRepository;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.spi.ClassDefinitionService;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private PaymentGatewayClient paymentGatewayClient;

    @Mock
    private ClassDefinitionService classDefinitionService;

    @Mock
    private TimetableService timetableService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void createBooking_initializesPaymentSession() {
        UUID instructorUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2024, 10, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2024, 10, 15, 10, 0);

        CreateBookingRequestDTO request = new CreateBookingRequestDTO(
                studentUuid,
                courseUuid,
                instructorUuid,
                start,
                end,
                new BigDecimal("50.00"),
                "USD",
                "Test booking"
        );

        when(availabilityService.isInstructorAvailable(instructorUuid, start, end)).thenReturn(true);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking booking = invocation.getArgument(0);
                    if (booking.getUuid() == null) {
                        booking.setUuid(UUID.randomUUID());
                    }
                    return booking;
                });

        when(paymentGatewayClient.initiatePayment(any(Booking.class)))
                .thenReturn(new PaymentSession("sess_123", "https://pay.local/sess_123", "placeholder"));

        var response = bookingService.createBooking(request);

        assertThat(response.status()).isEqualTo(BookingStatus.PAYMENT_REQUIRED);
        assertThat(response.availabilityBlockUuid()).isNull();
        assertThat(response.paymentSessionId()).isEqualTo("sess_123");

        verify(availabilityService).isInstructorAvailable(instructorUuid, start, end);
    }

    @Test
    void cancelBooking_releasesAvailability() {
        UUID bookingUuid = UUID.randomUUID();
        UUID blockUuid = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setUuid(bookingUuid);
        booking.setStatus(BookingStatus.PAYMENT_REQUIRED);
        booking.setAvailabilityBlockUuid(blockUuid);

        when(bookingRepository.findByUuid(bookingUuid)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookingService.cancelBooking(bookingUuid);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.availabilityBlockUuid()).isNull();
    }

    @Test
    void applyPaymentUpdate_failure_releasesAvailability() {
        UUID bookingUuid = UUID.randomUUID();
        UUID blockUuid = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setUuid(bookingUuid);
        booking.setStatus(BookingStatus.PAYMENT_REQUIRED);
        booking.setAvailabilityBlockUuid(blockUuid);

        when(bookingRepository.findByUuid(bookingUuid)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingPaymentUpdateRequestDTO request = new BookingPaymentUpdateRequestDTO("ref_1", "failed", "stripe");

        var response = bookingService.applyPaymentUpdate(bookingUuid, request);

        assertThat(response.status()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(response.availabilityBlockUuid()).isNull();
    }

    @Test
    void acceptBooking_afterPayment_createsScheduledInstanceAndEnrollment() {
        UUID bookingUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID instanceUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2024, 10, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2024, 10, 15, 10, 0);

        Booking booking = new Booking();
        booking.setUuid(bookingUuid);
        booking.setStudentUuid(studentUuid);
        booking.setInstructorUuid(instructorUuid);
        booking.setCourseUuid(courseUuid);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setStatus(BookingStatus.CONFIRMED);

        ClassDefinitionDTO classDefinition = new ClassDefinitionDTO(
                classDefinitionUuid,
                "Booking class",
                null,
                instructorUuid,
                null,
                courseUuid,
                null,
                null,
                ClassVisibility.PRIVATE,
                SessionFormat.INDIVIDUAL,
                start,
                end,
                LocationType.ONLINE,
                null,
                null,
                null,
                1,
                true,
                true,
                List.of(),
                null,
                null,
                null,
                null
        );

        ScheduledInstanceDTO instance = new ScheduledInstanceDTO(
                instanceUuid,
                classDefinitionUuid,
                instructorUuid,
                start,
                end,
                "UTC",
                "Booking session",
                "ONLINE",
                null,
                null,
                null,
                1,
                SchedulingStatus.SCHEDULED,
                null,
                null,
                null,
                null,
                null
        );

        EnrollmentDTO enrollment = new EnrollmentDTO(
                enrollmentUuid,
                instanceUuid,
                studentUuid,
                EnrollmentStatus.ENROLLED,
                null,
                null,
                null,
                null,
                null
        );

        when(bookingRepository.findByUuid(bookingUuid)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classDefinitionService.findActiveClassesForCourse(courseUuid)).thenReturn(List.of(new ClassDefinitionResponseDTO(classDefinition)));
        when(timetableService.scheduleClass(any(ScheduleRequestDTO.class))).thenReturn(instance);
        when(timetableService.enrollStudentInInstance(instanceUuid, studentUuid)).thenReturn(enrollment);

        var response = bookingService.acceptBooking(bookingUuid);

        assertThat(response.status()).isEqualTo(BookingStatus.ACCEPTED_CONFIRMED);
        assertThat(response.scheduledInstanceUuid()).isEqualTo(instanceUuid);
        assertThat(response.enrollmentUuid()).isEqualTo(enrollmentUuid);
        verify(timetableService).scheduleClass(any(ScheduleRequestDTO.class));
        verify(timetableService).enrollStudentInInstance(instanceUuid, studentUuid);
    }

    @Test
    void expireHolds_marksExpiredAndReleasesAvailability() {
        UUID blockUuid = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setUuid(UUID.randomUUID());
        booking.setStatus(BookingStatus.PAYMENT_REQUIRED);
        booking.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        booking.setAvailabilityBlockUuid(blockUuid);

        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(eq(BookingStatus.PAYMENT_REQUIRED), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        bookingService.expireHolds();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Booking>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository).saveAll(captor.capture());
        List<Booking> saved = captor.getValue();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(saved.get(0).getAvailabilityBlockUuid()).isNull();
    }
}
