package apps.sarafrika.elimika.booking.security;

import apps.sarafrika.elimika.booking.model.Booking;
import apps.sarafrika.elimika.booking.repository.BookingRepository;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("bookingSecurityService")
@RequiredArgsConstructor
@Slf4j
public class BookingSecurityService {

    private final BookingRepository bookingRepository;
    private final DomainSecurityService domainSecurityService;

    public boolean isBookingParticipant(UUID bookingUuid) {
        if (bookingUuid == null) {
            return false;
        }

        Booking booking = bookingRepository.findByUuid(bookingUuid).orElse(null);
        if (booking == null) {
            log.debug("Booking not found for UUID {}", bookingUuid);
            return false;
        }

        return domainSecurityService.isStudentWithUuid(booking.getStudentUuid())
                || domainSecurityService.isInstructorWithUuid(booking.getInstructorUuid());
    }

    public boolean isBookingParticipantOrAdmin(UUID bookingUuid) {
        if (domainSecurityService.isOrganizationAdmin()) {
            return true;
        }
        return isBookingParticipant(bookingUuid);
    }
}
