package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;

import java.util.Objects;

/** Copies basics between the shared professional profile and the instructor row's synced copy. */
public final class InstructorBasics {

    private InstructorBasics() {
    }

    public static boolean matches(Instructor instructor, ProfessionalProfileDTO basics) {
        return Objects.equals(instructor.getBio(), basics.bio())
                && Objects.equals(instructor.getProfessionalHeadline(), basics.professionalHeadline())
                && Objects.equals(instructor.getWebsite(), basics.website())
                && Objects.equals(instructor.getLocationName(), basics.locationName())
                && sameNumber(instructor.getLatitude(), basics.latitude())
                && sameNumber(instructor.getLongitude(), basics.longitude());
    }

    public static void copy(ProfessionalProfileDTO basics, Instructor instructor) {
        instructor.setBio(basics.bio());
        instructor.setProfessionalHeadline(basics.professionalHeadline());
        instructor.setWebsite(basics.website());
        instructor.setLocationName(basics.locationName());
        instructor.setLatitude(basics.latitude());
        instructor.setLongitude(basics.longitude());
    }

    private static boolean sameNumber(java.math.BigDecimal a, java.math.BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
