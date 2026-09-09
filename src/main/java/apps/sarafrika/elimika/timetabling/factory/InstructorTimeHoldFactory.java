package apps.sarafrika.elimika.timetabling.factory;

import apps.sarafrika.elimika.timetabling.model.InstructorTimeHold;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorTimeHoldFactory {

    public static InstructorTimeHoldDTO toDTO(InstructorTimeHold entity) {
        if (entity == null) {
            return null;
        }
        return new InstructorTimeHoldDTO(
                entity.getUuid(),
                entity.getInstructorUuid(),
                entity.getJobUuid(),
                entity.getApplicationUuid(),
                entity.getOrganisationUuid(),
                null,
                entity.getTitle(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getTimezone(),
                entity.getStatus(),
                entity.getClassDefinitionUuid(),
                entity.getScheduledInstanceUuid()
        );
    }

    public static List<InstructorTimeHoldDTO> toDTOList(List<InstructorTimeHold> entities) {
        return entities == null ? List.of() : entities.stream().map(InstructorTimeHoldFactory::toDTO).toList();
    }
}
