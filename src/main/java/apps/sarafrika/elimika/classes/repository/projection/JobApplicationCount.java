package apps.sarafrika.elimika.classes.repository.projection;

import java.util.UUID;

/**
 * How many applications one marketplace job has received.
 */
public record JobApplicationCount(UUID jobUuid, long applicationCount) {
}
