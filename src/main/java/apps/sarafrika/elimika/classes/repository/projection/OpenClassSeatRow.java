package apps.sarafrika.elimika.classes.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One open class, reduced to what a catalogue summary needs: which course it delivers, its fee and its
 * seat cap (to tell whether it is full once enrolments are counted).
 *
 * @param classUuid       the class definition
 * @param courseUuid      the course it delivers
 * @param fee             its sale price, null when not set
 * @param maxParticipants its seat cap, null when unknown
 */
public record OpenClassSeatRow(UUID classUuid, UUID courseUuid, BigDecimal fee, Integer maxParticipants) {
}
