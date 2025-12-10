package apps.sarafrika.elimika.booking.spi;

import apps.sarafrika.elimika.booking.dto.BookingPaymentUpdateRequestDTO;
import apps.sarafrika.elimika.booking.dto.BookingResponseDTO;
import apps.sarafrika.elimika.booking.dto.CreateBookingRequestDTO;

import java.util.UUID;

public interface BookingService {

    BookingResponseDTO createBooking(CreateBookingRequestDTO request);

    BookingResponseDTO getBooking(UUID bookingUuid);

    BookingResponseDTO cancelBooking(UUID bookingUuid);

    BookingResponseDTO applyPaymentUpdate(UUID bookingUuid, BookingPaymentUpdateRequestDTO request);

    void expireHolds();
}
