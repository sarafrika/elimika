package apps.sarafrika.elimika.shared.spi;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A class a visitor can still join, reduced to what a public course page may show.
 * <p>
 * Built for anonymous readers, so it is narrow by construction: no coordinates, no meeting link, no
 * instructor or organisation identifiers, no instructor pay and no revenue terms. The fee is the
 * class's sale price, which is the sticker price a learner is charged and therefore public.
 *
 * @param uuid                 the class definition
 * @param title                display title
 * @param locationType         in person, online or hybrid
 * @param sessionFormat        group or one-to-one
 * @param locationName         the human-readable place label as stored; never coordinates
 * @param salePrice            the fee a learner pays, null when not set
 * @param maxParticipants      seats offered, null when unknown
 * @param seatsLeft            seats offered minus live enrolments, never negative; null when unknown
 * @param startsOn             first teaching day, null when unknown
 * @param endsOn               last teaching day, null when open-ended
 * @param registrationClosesOn last day, inclusive, enrolments are accepted
 * @param branchName           the training branch it is delivered at, null when none
 */
public record OpenClassListing(
        UUID uuid,
        String title,
        LocationType locationType,
        SessionFormat sessionFormat,
        String locationName,
        BigDecimal salePrice,
        Integer maxParticipants,
        Integer seatsLeft,
        LocalDate startsOn,
        LocalDate endsOn,
        LocalDate registrationClosesOn,
        String branchName
) {
}
