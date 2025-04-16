package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.TrainingExperienceDTO;
import apps.sarafrika.elimika.tenancy.entity.TrainingExperience;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TrainingExperienceFactory {

    public static TrainingExperienceDTO toDTO(TrainingExperience trainingExperience) {
        return new TrainingExperienceDTO(
                trainingExperience.getOrganisationName(),
                trainingExperience.getJobTitle(),
                trainingExperience.getWorkDescription(),
                trainingExperience.getStartDate(),
                trainingExperience.getEndDate(),
                trainingExperience.getUserUuid()
        );
    }

    public static TrainingExperience toEntity(TrainingExperienceDTO dto) {
        TrainingExperience trainingExperience = new TrainingExperience();
        trainingExperience.setOrganisationName(dto.organisationName());
        trainingExperience.setJobTitle(dto.jobTitle());
        trainingExperience.setWorkDescription(dto.workDescription());
        trainingExperience.setStartDate(dto.startDate());
        trainingExperience.setEndDate(dto.endDate());
        trainingExperience.setUserUuid(dto.userUuid());
        return trainingExperience;
    }
}
