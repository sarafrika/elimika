package apps.sarafrika.elimika.classes.factory;

import apps.sarafrika.elimika.classes.dto.RecurrencePatternDTO;
import apps.sarafrika.elimika.classes.model.RecurrencePattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecurrencePatternFactory {

    public static RecurrencePatternDTO toDTO(RecurrencePattern entity) {
        if (entity == null) {
            return null;
        }
        return new RecurrencePatternDTO(
                entity.getUuid(),
                entity.getRecurrenceType(),
                entity.getIntervalValue(),
                entity.getDaysOfWeek(),
                entity.getDayOfMonth(),
                entity.getEndDate(),
                entity.getOccurrenceCount()
        );
    }
}
