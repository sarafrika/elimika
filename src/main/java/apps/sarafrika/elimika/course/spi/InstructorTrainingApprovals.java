package apps.sarafrika.elimika.course.spi;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Every course and program an instructor is approved to train, each with its approved rate card. */
public record InstructorTrainingApprovals(Map<UUID, ApprovedRateCard> courses, Map<UUID, ApprovedRateCard> programs) {

    public InstructorTrainingApprovals {
        courses = courses == null ? Map.of() : Map.copyOf(courses);
        programs = programs == null ? Map.of() : Map.copyOf(programs);
    }

    /** One approved rate card; a cell that is unpriced or not offered resolves to empty. */
    @FunctionalInterface
    public interface ApprovedRateCard {
        Optional<BigDecimal> rate(SessionFormat sessionFormat, LocationType locationType, RateBasis basis);
    }

    public boolean approvedForCourse(UUID courseUuid) {
        return courseUuid != null && courses.containsKey(courseUuid);
    }

    public boolean approvedForProgram(UUID programUuid) {
        return programUuid != null && programs.containsKey(programUuid);
    }

    public Optional<BigDecimal> courseRate(UUID courseUuid, SessionFormat sessionFormat, LocationType locationType,
                                           RateBasis basis) {
        return rate(approvedForCourse(courseUuid) ? courses.get(courseUuid) : null, sessionFormat, locationType, basis);
    }

    public Optional<BigDecimal> programRate(UUID programUuid, SessionFormat sessionFormat, LocationType locationType,
                                            RateBasis basis) {
        return rate(approvedForProgram(programUuid) ? programs.get(programUuid) : null, sessionFormat, locationType, basis);
    }

    private static Optional<BigDecimal> rate(ApprovedRateCard card, SessionFormat sessionFormat,
                                             LocationType locationType, RateBasis basis) {
        if (card == null || sessionFormat == null) {
            return Optional.empty();
        }
        return card.rate(sessionFormat, locationType, basis);
    }
}
