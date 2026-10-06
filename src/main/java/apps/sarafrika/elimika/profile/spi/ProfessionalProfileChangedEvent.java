package apps.sarafrika.elimika.profile.spi;

import java.util.UUID;

/**
 * Published inside the writing transaction whenever a section of a user's profile changes.
 * {@code basics} is set only for {@link ProfileSection#BASICS}, so domain rows can sync their copy.
 */
public record ProfessionalProfileChangedEvent(UUID userUuid, ProfileSection section, ProfessionalProfileDTO basics) {
}
