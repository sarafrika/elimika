package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.AcademicTierDTO;
import apps.sarafrika.elimika.tenancy.entity.AcademicTier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AcademicTierFactory {

    public static AcademicTierDTO toDTO(AcademicTier tier) {
        return new AcademicTierDTO(
                tier.getUuid(),
                tier.getName(),
                tier.getTierOrder(),
                tier.getEducationSystem(),
                tier.getOrganisationUuid(),
                tier.isActive(),
                tier.getDescription()
        );
    }
}
