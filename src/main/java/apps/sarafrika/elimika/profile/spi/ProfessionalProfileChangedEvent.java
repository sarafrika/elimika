package apps.sarafrika.elimika.profile.spi;

import java.util.UUID;

/** Published in the writing transaction when a profile section changes; {@code basics} is set for BASICS only. */
public record ProfessionalProfileChangedEvent(UUID userUuid, ProfileSection section, ProfessionalProfileDTO basics) {
}
