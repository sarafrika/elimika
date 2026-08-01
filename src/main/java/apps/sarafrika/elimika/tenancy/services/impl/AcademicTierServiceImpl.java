package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.dto.AcademicTierDTO;
import apps.sarafrika.elimika.tenancy.factory.AcademicTierFactory;
import apps.sarafrika.elimika.tenancy.repository.AcademicTierRepository;
import apps.sarafrika.elimika.tenancy.services.AcademicTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AcademicTierServiceImpl implements AcademicTierService {

    /** The only curriculum seeded today; used when the caller does not name one. */
    static final String DEFAULT_EDUCATION_SYSTEM = "KE";

    private final AcademicTierRepository academicTierRepository;

    @Override
    public List<AcademicTierDTO> getPlatformTiers(String educationSystem) {
        String system = (educationSystem == null || educationSystem.isBlank())
                ? DEFAULT_EDUCATION_SYSTEM
                : educationSystem.trim();

        // Ordering, the active filter and the platform-only restriction are all pushed into the
        // query: the frontend renders these straight into filter pills and must not have to sort
        // or filter a reference list itself.
        return academicTierRepository
                .findByEducationSystemIgnoreCaseAndOrganisationUuidIsNullAndActiveTrueOrderByTierOrderAsc(system)
                .stream()
                .map(AcademicTierFactory::toDTO)
                .toList();
    }
}
