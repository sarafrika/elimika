package apps.sarafrika.elimika.instructor.persistence;

import apps.sarafrika.elimika.instructor.dto.request.CreateAvailabilityPatternRequestDTO;
import apps.sarafrika.elimika.instructor.dto.request.UpdateAvailabilityPatternRequestDTO;

public class AvailabilityPatternFactory {

    public static AvailabilityPattern create(Long instructorId, CreateAvailabilityPatternRequestDTO createAvailabilityPatternRequestDTO) {

        return AvailabilityPattern.builder()
                .instructorId(instructorId)
                .patternType(createAvailabilityPatternRequestDTO.patternType())
                .startDate(createAvailabilityPatternRequestDTO.startDate())
                .endDate(createAvailabilityPatternRequestDTO.endDate())
                .build();
    }

    public static void update(UpdateAvailabilityPatternRequestDTO updateAvailabilityPatternRequestDTO, AvailabilityPattern availabilityPattern) {
        availabilityPattern.setPatternType(updateAvailabilityPatternRequestDTO.patternType());
        availabilityPattern.setStartDate(updateAvailabilityPatternRequestDTO.startDate());
        availabilityPattern.setEndDate(updateAvailabilityPatternRequestDTO.endDate());
    }
}
