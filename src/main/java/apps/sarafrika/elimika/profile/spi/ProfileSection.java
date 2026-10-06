package apps.sarafrika.elimika.profile.spi;

import java.util.Locale;

/** The sections of a professional profile, named as they appear in the API path. */
public enum ProfileSection {
    BASICS("basics", false),
    SKILLS("skills", true),
    EDUCATION("education", false),
    EXPERIENCE("experience", false),
    MEMBERSHIPS("memberships", false),
    CERTIFICATIONS("certifications", true),
    PORTFOLIO("portfolio", false),
    COMPETENCIES("competencies", true),
    ACHIEVEMENTS("achievements", false),
    DOCUMENTS("documents", true);

    private final String path;
    private final boolean verifiable;

    ProfileSection(String path, boolean verifiable) {
        this.path = path;
        this.verifiable = verifiable;
    }

    public String path() {
        return path;
    }

    /** Whether a platform admin can mark items of this section VERIFIED or REJECTED. */
    public boolean verifiable() {
        return verifiable;
    }

    public static ProfileSection fromPath(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (ProfileSection section : values()) {
            if (section.path.equals(normalized) || section.name().equalsIgnoreCase(normalized)) {
                return section;
            }
        }
        throw new IllegalArgumentException("Unknown profile section: " + value);
    }
}
