package apps.sarafrika.elimika.booking.service.impl;

import apps.sarafrika.elimika.availability.dto.AvailabilitySlotDTO;
import apps.sarafrika.elimika.availability.dto.BlockedTimeSlotRequestDTO;
import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.booking.dto.BookingPaymentUpdateRequestDTO;
import apps.sarafrika.elimika.booking.dto.CreateBookingRequestDTO;
import apps.sarafrika.elimika.booking.model.Booking;
import apps.sarafrika.elimika.booking.payment.PaymentGatewayClient;
import apps.sarafrika.elimika.booking.payment.PaymentSession;
import apps.sarafrika.elimika.booking.repository.BookingRepository;
import apps.sarafrika.elimika.shared.enums.AvailabilityType;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private PaymentGatewayClient paymentGatewayClient;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void createBooking_blocksAvailability_andReturnsSession() {
        UUID instructorUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2024, 10, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2024, 10, 15, 10, 0);
        UUID blockUuid = UUID.randomUUID();

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
        AvailabilitySlotDTO blockedSlot = new AvailabilitySlotDTO(
                blockUuid,
                instructorUuid,
                AvailabilityType.CUSTOM,
                null,
                null,
                LocalDate.from(start),
                LocalTime.from(start),
                LocalTime.from(end),
                "BLOCKED_TIME_SLOT",
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "#FFD93D"
        );
        when(availabilityService.blockTimeSlots(eq(instructorUuid), anyList()))
                .thenReturn(List.of(blockedSlot));

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
        assertThat(response.availabilityBlockUuid()).isEqualTo(blockUuid);
        assertThat(response.paymentSessionId()).isEqualTo("sess_123");

        verify(availabilityService).blockTimeSlots(eq(instructorUuid), anyList());
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
        verify(availabilityService).removeBlockedSlot(blockUuid);
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
        verify(availabilityService).removeBlockedSlot(blockUuid);
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
        verify(availabilityService).removeBlockedSlot(blockUuid);
    }
}
