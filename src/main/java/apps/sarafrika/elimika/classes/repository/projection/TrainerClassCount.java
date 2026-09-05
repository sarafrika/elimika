package apps.sarafrika.elimika.classes.repository.projection;

import java.util.UUID;

/**
 * How many active classes one trainer — an instructor or an organisation — runs on a course.
 *
 * @param trainerUuid the instructor or organisation the classes belong to
 * @param classCount  the number of active class definitions
 */
public record TrainerClassCount(UUID trainerUuid, long classCount) {
}
