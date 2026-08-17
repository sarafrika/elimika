package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.services.UserService;
import apps.sarafrika.elimika.tenancy.spi.OrganisationAffiliationService;
import apps.sarafrika.elimika.tenancy.util.enums.ConsentSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of {@link OrganisationAffiliationService}.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-08-13
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationAffiliationServiceImpl implements OrganisationAffiliationService {

    private static final String INSTRUCTOR_DOMAIN = "instructor";

    private final UserService userService;
    private final UserOrganisationDomainMappingRepository mappingRepository;

    @Override
    @Transactional
    public boolean affiliateHiredInstructor(UUID userUuid, UUID organisationUuid, UUID branchUuid) {
        if (userUuid == null || organisationUuid == null) {
            throw new IllegalArgumentException(
                    "Both the instructor's user and the hiring organisation are required to create an affiliation.");
        }

        // Only one active affiliation per (user, organisation) is allowed, and assigning
        // would overwrite whatever role they already hold. An existing member keeps theirs.
        Optional<UserOrganisationDomainMapping> existing =
                mappingRepository.findActiveByUserAndOrganisation(userUuid, organisationUuid);
        if (existing.isPresent()) {
            log.debug("User {} is already affiliated with organisation {}; leaving the existing affiliation intact",
                    userUuid, organisationUuid);
            return false;
        }

        userService.assignUserToOrganisation(userUuid, organisationUuid, INSTRUCTOR_DOMAIN, branchUuid);
        stampConsent(userUuid, organisationUuid);

        log.info("Affiliated hired instructor {} with organisation {}", userUuid, organisationUuid);
        return true;
    }

    /**
     * Records that the instructor consented to this affiliation by applying for the job.
     */
    private void stampConsent(UUID userUuid, UUID organisationUuid) {
        Optional<UserOrganisationDomainMapping> mapping =
                mappingRepository.findActiveByUserAndOrganisation(userUuid, organisationUuid);

        if (mapping.isEmpty()) {
            log.warn("No active mapping found to stamp consent on for user {} in organisation {}",
                    userUuid, organisationUuid);
            return;
        }

        UserOrganisationDomainMapping affiliation = mapping.get();
        affiliation.setConsentGrantedAt(LocalDateTime.now());
        affiliation.setConsentSource(ConsentSource.SELF_JOIN);
        affiliation.setConsentGrantedByUserUuid(userUuid);
        mappingRepository.save(affiliation);
    }
}
