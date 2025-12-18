package apps.sarafrika.elimika.booking.spi;

import apps.sarafrika.elimika.booking.dto.BookingPaymentRequestDTO;
import apps.sarafrika.elimika.booking.dto.BookingPaymentSessionDTO;
import apps.sarafrika.elimika.booking.dto.BookingPaymentUpdateRequestDTO;
import apps.sarafrika.elimika.booking.dto.BookingResponseDTO;
import apps.sarafrika.elimika.booking.dto.CreateBookingRequestDTO;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingService {

    BookingResponseDTO createBooking(CreateBookingRequestDTO request);

    BookingResponseDTO getBooking(UUID bookingUuid);

    BookingResponseDTO cancelBooking(UUID bookingUuid);

    BookingResponseDTO applyPaymentUpdate(UUID bookingUuid, BookingPaymentUpdateRequestDTO request);

    BookingPaymentSessionDTO requestPayment(UUID bookingUuid, BookingPaymentRequestDTO request);

    Page<BookingResponseDTO> getBookingsForStudent(UUID studentUuid, BookingStatus status, Pageable pageable);

    Page<BookingResponseDTO> getBookingsForInstructor(UUID instructorUuid, BookingStatus status, Pageable pageable);

    void expireHolds();
}
