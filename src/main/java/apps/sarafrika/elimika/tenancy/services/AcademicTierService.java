package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.AcademicTierDTO;

import java.util.List;

/**
 * Read-only access to the academic tier catalogue (schooling levels).
 * <p>
 * There is deliberately no create/update/delete: the catalogue is seeded reference data that
 * student groups point at, and letting one tenant edit it would change what every other tenant's
 * groups mean.
 */
public interface AcademicTierService {

    /**
     * The active platform tiers for an education system, in curriculum order.
     *
     * @param educationSystem curriculum code, e.g. {@code KE}
     */
    List<AcademicTierDTO> getPlatformTiers(String educationSystem);
}
