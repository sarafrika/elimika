package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.TrainingBranchDTO;
import apps.sarafrika.elimika.tenancy.entity.TrainingBranch;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TrainingBranchFactory {

    public static TrainingBranchDTO toDTO(TrainingBranch trainingBranch) {
        return new TrainingBranchDTO(
                trainingBranch.getUuid(),
                trainingBranch.getOrganisationUuid(),
                trainingBranch.getBranchName(),
                trainingBranch.getAddress(),
                trainingBranch.getPocUserUuid(),
                trainingBranch.getPocName(),
                trainingBranch.getPocEmail(),
                trainingBranch.getPocTelephone(),
                trainingBranch.isActive(),
                trainingBranch.getCreatedDate(),
                trainingBranch.getLastModifiedDate()
        );
    }

    public static TrainingBranch toEntity(TrainingBranchDTO trainingBranchDTO) {
        TrainingBranch trainingBranch = new TrainingBranch();
        trainingBranch.setUuid(trainingBranchDTO.uuid());
        trainingBranch.setOrganisationUuid(trainingBranchDTO.organisationUuid());
        trainingBranch.setBranchName(trainingBranchDTO.branchName());
        trainingBranch.setAddress(trainingBranchDTO.address());
        trainingBranch.setPocUserUuid(trainingBranchDTO.pocUserUuid());
        trainingBranch.setPocName(trainingBranchDTO.pocName());
        trainingBranch.setPocEmail(trainingBranchDTO.pocEmail());
        trainingBranch.setPocTelephone(trainingBranchDTO.pocTelephone());
        trainingBranch.setActive(trainingBranchDTO.active());
        return trainingBranch;
    }
}