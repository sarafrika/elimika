package apps.sarafrika.elimika.coursecreator.internal;

import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;

import java.math.BigDecimal;
import java.util.Objects;

/** Copies basics between the shared professional profile and the course creator row's synced copy. */
public final class CourseCreatorBasics {

    private CourseCreatorBasics() {
    }

    public static boolean matches(CourseCreator creator, ProfessionalProfileDTO basics) {
        return Objects.equals(creator.getBio(), basics.bio())
                && Objects.equals(creator.getProfessionalHeadline(), basics.professionalHeadline())
                && Objects.equals(creator.getWebsite(), basics.website())
                && Objects.equals(creator.getLocationName(), basics.locationName())
                && sameNumber(creator.getLatitude(), basics.latitude())
                && sameNumber(creator.getLongitude(), basics.longitude());
    }

    public static void copy(ProfessionalProfileDTO basics, CourseCreator creator) {
        creator.setBio(basics.bio());
        creator.setProfessionalHeadline(basics.professionalHeadline());
        creator.setWebsite(basics.website());
        creator.setLocationName(basics.locationName());
        creator.setLatitude(basics.latitude());
        creator.setLongitude(basics.longitude());
    }

    private static boolean sameNumber(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
