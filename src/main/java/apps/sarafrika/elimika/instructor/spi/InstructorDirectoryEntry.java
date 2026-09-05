package apps.sarafrika.elimika.instructor.spi;

import java.util.UUID;

/**
 * The three facts a public listing needs about an instructor: what to call them, roughly where they
 * work, and whether the platform has vetted them.
 * <p>
 * Deliberately narrower than {@link InstructorDTO}, which also carries the bio, website, and — the
 * reason this record exists — latitude and longitude. {@code InstructorDTO.getFormattedLocation()}
 * falls back to formatting those coordinates when no place name is set; a directory must not, so
 * {@link #locationName} is the stored place name or nothing at all.
 *
 * @param instructorUuid the instructor identifier
 * @param displayName    the instructor's full name, as they gave it
 * @param locationName   the town or locality they work from, null when they have not named one
 * @param adminVerified  whether an administrator has verified the instructor
 */
public record InstructorDirectoryEntry(UUID instructorUuid,
                                       String displayName,
                                       String locationName,
                                       boolean adminVerified) {
}
