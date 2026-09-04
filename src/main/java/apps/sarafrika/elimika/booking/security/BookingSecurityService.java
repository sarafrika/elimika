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

    /**
     * True only for the two parties named on the booking: the learner who requested it and the
     * instructor who is being booked.
     */
    public boolean isBookingParticipant(UUID bookingUuid) {
        Booking booking = findBooking(bookingUuid);
        return booking != null && isParty(booking);
    }

    /**
     * True for either party to the booking, for an administrator of an organisation one of those
     * parties belongs to, and for platform admins.
     * <p>
     * This used to fall through to {@link DomainSecurityService#isOrganizationAdmin()}, which asks
     * only whether the caller administers <em>something, somewhere</em> — it never looks at whose
     * booking is being read. Any user holding the {@code admin} domain in any organisation could
     * therefore read every booking on the platform, including its price, currency, payment session
     * id, payment reference and payment engine. The administrative branch now resolves the users
     * owning the booking's student and instructor profiles and requires admin standing in an
     * organisation one of them actually belongs to, so administrative reach is scoped to the
     * parties rather than granted globally.
     */
    public boolean isBookingParticipantOrAdmin(UUID bookingUuid) {
        Booking booking = findBooking(bookingUuid);
        if (booking == null) {
            return false;
        }
        return isParty(booking)
                || domainSecurityService.isPlatformAdmin()
                || domainSecurityService.administersOrganisationOfStudent(booking.getStudentUuid())
                || domainSecurityService.administersOrganisationOfInstructor(booking.getInstructorUuid());
    }

    /**
     * True only for the instructor named on the booking; accepting and declining stay with them.
     */
    public boolean isBookingInstructor(UUID bookingUuid) {
        Booking booking = findBooking(bookingUuid);
        return booking != null && domainSecurityService.isInstructorWithUuid(booking.getInstructorUuid());
    }

    private boolean isParty(Booking booking) {
        return domainSecurityService.isStudentWithUuid(booking.getStudentUuid())
                || domainSecurityService.isInstructorWithUuid(booking.getInstructorUuid());
    }

    private Booking findBooking(UUID bookingUuid) {
        if (bookingUuid == null) {
            return null;
        }
        Booking booking = bookingRepository.findByUuid(bookingUuid).orElse(null);
        if (booking == null) {
            log.debug("Booking not found for UUID {}", bookingUuid);
        }
        return booking;
    }
}
