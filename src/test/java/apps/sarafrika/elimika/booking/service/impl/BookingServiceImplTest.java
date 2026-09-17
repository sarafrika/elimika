package apps.sarafrika.elimika.booking.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.booking.dto.BookingPaymentUpdateRequestDTO;
import apps.sarafrika.elimika.booking.dto.BookingResponseDTO;
import apps.sarafrika.elimika.booking.dto.CreateBookingRequestDTO;
import apps.sarafrika.elimika.booking.model.Booking;
import apps.sarafrika.elimika.booking.payment.PaymentGatewayClient;
import apps.sarafrika.elimika.booking.payment.PaymentSession;
import apps.sarafrika.elimika.booking.repository.BookingRepository;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.spi.ClassDefinitionService;
import apps.sarafrika.elimika.course.spi.ApprovedTrainingRate;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldDTO;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldService;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private CourseInfoService courseInfoService;

    @Mock
    private InstructorTimeHoldService instructorTimeHoldService;

    @Mock
    private CourseTrainingApprovalSpi courseTrainingApprovalSpi;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void createBooking_perHour_chargesTheApprovedRateForTheSessionLength() {
        UUID instructorUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        LocalDateTime start = LocalDate.now(ZoneOffset.UTC).plusDays(7).atTime(9, 0);
        LocalDateTime end = start.plusMinutes(100);
        CreateBookingRequestDTO request = request(studentUuid, courseUuid, instructorUuid, start, end,
                SessionFormat.INDIVIDUAL, LocationType.ONLINE, RateBasis.PER_HOUR, null);

        allowBooking(instructorUuid, courseUuid, start, end);
        approvedRate(courseUuid, instructorUuid, SessionFormat.INDIVIDUAL, LocationType.ONLINE, RateBasis.PER_HOUR, "1000");
        savesInMemory();
        when(paymentGatewayClient.initiatePayment(any(Booking.class)))
                .thenReturn(new PaymentSession("sess_123", "https://pay.local/sess_123", "placeholder"));

        var response = bookingService.createBooking(request);

        assertThat(response.status()).isEqualTo(BookingStatus.PAYMENT_REQUIRED);
        assertThat(response.priceAmount()).isEqualByComparingTo("1666.67");
        assertThat(response.priceAmount().scale()).isEqualTo(2);
        assertThat(response.currency()).isEqualTo("KES");
        assertThat(response.unitRate()).isEqualByComparingTo("1000");
        assertThat(response.rateBasis()).isEqualTo(RateBasis.PER_HOUR);
        assertThat(response.trainingFormat()).isEqualTo(SessionFormat.INDIVIDUAL);
        assertThat(response.deliveryMode()).isEqualTo(LocationType.ONLINE);
        assertThat(response.holdExpiresAt()).isNotNull();
        assertThat(response.availabilityBlockUuid()).isNull();
        assertThat(response.paymentSessionId()).isEqualTo("sess_123");

        verify(availabilityService).isInstructorAvailable(instructorUuid, start, end);
    }

    @Test
    void createBooking_perSession_chargesTheRateWhateverTheLength() {
        UUID instructorUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        LocalDateTime start = LocalDate.now(ZoneOffset.UTC).plusDays(7).atTime(9, 0);
        LocalDateTime end = start.plusMinutes(150);
        CreateBookingRequestDTO request = request(UUID.randomUUID(), courseUuid, instructorUuid, start, end,
                SessionFormat.GROUP, LocationType.IN_PERSON, RateBasis.PER_SESSION, "Africa/Nairobi");

        allowBooking(instructorUuid, courseUuid, start, end);
        approvedRate(courseUuid, instructorUuid, SessionFormat.GROUP, LocationType.IN_PERSON, RateBasis.PER_SESSION, "1800");
        savesInMemory();
        when(paymentGatewayClient.initiatePayment(any(Booking.class)))
                .thenReturn(new PaymentSession("sess_1", "https://pay.local/sess_1", "placeholder"));

        var response = bookingService.createBooking(request);

        assertThat(response.priceAmount()).isEqualByComparingTo("1800.00");
        assertThat(response.rateBasis()).isEqualTo(RateBasis.PER_SESSION);
        assertThat(response.status()).isEqualTo(BookingStatus.PAYMENT_REQUIRED);
    }

    @Test
    void createBooking_hybrid_isPricedFromTheInPersonRateTheCardResolves() {
        UUID instructorUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        LocalDateTime start = LocalDate.now(ZoneOffset.UTC).plusDays(7).atTime(9, 0);
        LocalDateTime end = start.plusHours(1);
        CreateBookingRequestDTO request = request(UUID.randomUUID(), courseUuid, instructorUuid, start, end,
                SessionFormat.INDIVIDUAL, LocationType.HYBRID, RateBasis.PER_SESSION, null);

        allowBooking(instructorUuid, courseUuid, start, end);
        // The course SPI owns the hybrid -> in-person mapping; the booking must ask for HYBRID, not guess a cell.
        approvedRate(courseUuid, instructorUuid, SessionFormat.INDIVIDUAL, LocationType.HYBRID, RateBasis.PER_SESSION, "4000");
        savesInMemory();
        when(paymentGatewayClient.initiatePayment(any(Booking.class)))
                .thenReturn(new PaymentSession("sess_2", "https://pay.local/sess_2", "placeholder"));

        var response = bookingService.createBooking(request);

        assertThat(response.priceAmount()).isEqualByComparingTo("4000.00");
        assertThat(response.deliveryMode()).isEqualTo(LocationType.HYBRID);
        verify(courseTrainingApprovalSpi, never()).resolveInstructorRateWithCurrency(
                any(), any(), any(), eq(LocationType.ONLINE), any());
    }

    @Test
    void createBooking_withoutAnApprovedRate_isRefusedBeforeAnythingIsSaved() {
        UUID instructorUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        LocalDateTime start = LocalDate.now(ZoneOffset.UTC).plusDays(7).atTime(9, 0);
        LocalDateTime end = start.plusHours(1);
        CreateBookingRequestDTO request = request(UUID.randomUUID(), courseUuid, instructorUuid, start, end,
                SessionFormat.GROUP, LocationType.ONLINE, RateBasis.PER_DAY, null);

        allowBooking(instructorUuid, courseUuid, start, end);
        when(courseTrainingApprovalSpi.resolveInstructorRateWithCurrency(
                courseUuid, instructorUuid, SessionFormat.GROUP, LocationType.ONLINE, RateBasis.PER_DAY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("This instructor has no approved per day rate for group online training on this course.");
        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(paymentGatewayClient);
    }

    @Test
    void createBooking_withAnUnknownTimezone_isRefusedBeforeAnyLookup() {
        LocalDateTime start = LocalDate.now(ZoneOffset.UTC).plusDays(7).atTime(9, 0);
        CreateBookingRequestDTO request = request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                start, start.plusHours(1), SessionFormat.GROUP, LocationType.ONLINE, RateBasis.PER_DAY, "Mars/Olympus");

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid IANA timezone: Mars/Olympus");
        verifyNoInteractions(availabilityService, courseTrainingApprovalSpi, bookingRepository, paymentGatewayClient);
    }

    @Test
    void createBooking_perDay_chargesTheFirstSessionOfEachLocalClassDayOnly() {
        UUID studentUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        LocalDate utcDay = LocalDate.now(ZoneOffset.UTC).plusDays(7);
        // Nairobi is UTC+3: 22:00 UTC is 01:00 the next local day, the same local day as 06:00 UTC after it.
        LocalDateTime lateUtc = utcDay.atTime(22, 0);
        LocalDateTime nextMorningUtc = utcDay.plusDays(1).atTime(6, 0);
        LocalDateTime nextLateUtc = utcDay.plusDays(1).atTime(22, 0);

        approvedRate(courseUuid, instructorUuid, SessionFormat.INDIVIDUAL, LocationType.IN_PERSON, RateBasis.PER_DAY, "9000");
        List<Booking> stored = savesInMemory();
        when(bookingRepository.existsChargedBookingStartingBetween(eq(studentUuid), eq(instructorUuid), eq(courseUuid),
                eq(RateBasis.PER_DAY), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    Collection<BookingStatus> statuses = invocation.getArgument(4);
                    LocalDateTime from = invocation.getArgument(5);
                    LocalDateTime to = invocation.getArgument(6);
                    return stored.stream().anyMatch(booking -> statuses.contains(booking.getStatus())
                            && booking.getPriceAmount().signum() > 0
                            && !booking.getStartTime().isBefore(from) && booking.getStartTime().isBefore(to));
                });
        when(paymentGatewayClient.initiatePayment(any(Booking.class)))
                .thenReturn(new PaymentSession("sess_day", "https://pay.local/sess_day", "placeholder"));

        var first = book(studentUuid, courseUuid, instructorUuid, lateUtc);
        var followOn = book(studentUuid, courseUuid, instructorUuid, nextMorningUtc);
        var nextLocalDay = book(studentUuid, courseUuid, instructorUuid, nextLateUtc);

        assertThat(first.priceAmount()).isEqualByComparingTo("9000.00");
        assertThat(first.status()).isEqualTo(BookingStatus.PAYMENT_REQUIRED);

        assertThat(followOn.priceAmount()).isEqualByComparingTo("0.00");
        assertThat(followOn.unitRate()).isEqualByComparingTo("9000");
        assertThat(followOn.status()).as("nothing to collect, so no payment is demanded").isEqualTo(BookingStatus.CONFIRMED);
        assertThat(followOn.holdExpiresAt()).isNull();
        assertThat(followOn.paymentSessionId()).isNull();

        assertThat(nextLocalDay.priceAmount()).isEqualByComparingTo("9000.00");
        verify(paymentGatewayClient, times(2)).initiatePayment(any(Booking.class));
        verify(bookingRepository, times(2).description("both sessions fall on the same Nairobi day"))
                .existsChargedBookingStartingBetween(studentUuid, instructorUuid, courseUuid,
                        RateBasis.PER_DAY, EnumSet.of(BookingStatus.PAYMENT_REQUIRED, BookingStatus.CONFIRMED,
                                BookingStatus.ACCEPTED, BookingStatus.ACCEPTED_CONFIRMED),
                        utcDay.atTime(21, 0), utcDay.plusDays(1).atTime(21, 0));
    }

    private BookingResponseDTO book(UUID studentUuid, UUID courseUuid, UUID instructorUuid, LocalDateTime start) {
        LocalDateTime end = start.plusHours(1);
        allowBooking(instructorUuid, courseUuid, start, end);
        return bookingService.createBooking(request(studentUuid, courseUuid, instructorUuid, start, end,
                SessionFormat.INDIVIDUAL, LocationType.IN_PERSON, RateBasis.PER_DAY, "Africa/Nairobi"));
    }

    private static CreateBookingRequestDTO request(UUID studentUuid, UUID courseUuid, UUID instructorUuid,
                                                   LocalDateTime start, LocalDateTime end, SessionFormat format,
                                                   LocationType delivery, RateBasis basis, String timezone) {
        return new CreateBookingRequestDTO(studentUuid, courseUuid, instructorUuid, start, end,
                format, delivery, basis, timezone, "Test booking");
    }

    private void allowBooking(UUID instructorUuid, UUID courseUuid, LocalDateTime start, LocalDateTime end) {
        when(availabilityService.isInstructorAvailable(instructorUuid, start, end)).thenReturn(true);
        lenient().when(courseInfoService.isCourseApproved(courseUuid)).thenReturn(true);
    }

    private void approvedRate(UUID courseUuid, UUID instructorUuid, SessionFormat format, LocationType delivery,
                              RateBasis basis, String rate) {
        when(courseTrainingApprovalSpi.resolveInstructorRateWithCurrency(courseUuid, instructorUuid, format, delivery, basis))
                .thenReturn(Optional.of(new ApprovedTrainingRate(new BigDecimal(rate), "KES")));
    }

    private List<Booking> savesInMemory() {
        List<Booking> stored = new ArrayList<>();
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            if (booking.getUuid() == null) {
                booking.setUuid(UUID.randomUUID());
                stored.add(booking);
            }
            return booking;
        });
        return stored;
    }

    @Test
    void createBooking_isRefusedWhenAHiredJobHoldsTheWindow() {
        UUID instructorUuid = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2024, 10, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2024, 10, 15, 10, 0);
        CreateBookingRequestDTO request = request(UUID.randomUUID(), UUID.randomUUID(), instructorUuid, start, end,
                SessionFormat.GROUP, LocationType.ONLINE, RateBasis.PER_SESSION, null);
        InstructorTimeHoldDTO firmHold = new InstructorTimeHoldDTO(
                UUID.randomUUID(), instructorUuid, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "Grade 5 Piano", start.minusMinutes(30), end.plusMinutes(30), "UTC",
                InstructorTimeHoldStatus.FIRM, null, null);

        when(availabilityService.isInstructorAvailable(instructorUuid, start, end)).thenReturn(true);
        when(instructorTimeHoldService.findBlockingHolds(instructorUuid, start, end, null))
                .thenReturn(List.of(firmHold));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Instructor has accepted a class job that holds the requested time range.");
        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(paymentGatewayClient);
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
                null,
                courseUuid,
                null,
                null,
                null,
                null,
                ClassVisibility.PRIVATE,
                SessionFormat.INDIVIDUAL,
                start,
                end,
                null,
                null,
                null,
                null,
                null,
                null,
                LocationType.ONLINE,
                null,
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
        when(courseInfoService.isCourseApproved(courseUuid)).thenReturn(true);
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

    @Test
    void createBookingRejectsUnapprovedCourse() {
        UUID instructorUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2024, 10, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2024, 10, 15, 10, 0);

        CreateBookingRequestDTO request = request(studentUuid, courseUuid, instructorUuid, start, end,
                SessionFormat.GROUP, LocationType.ONLINE, RateBasis.PER_SESSION, null);

        when(availabilityService.isInstructorAvailable(instructorUuid, start, end)).thenReturn(true);
        when(courseInfoService.isCourseApproved(courseUuid)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not approved for bookings");

        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(courseTrainingApprovalSpi);
    }
}
