package apps.sarafrika.elimika.tenancy.service;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.sms.Msisdn;
import apps.sarafrika.elimika.tenancy.dto.OrganisationPayoutDetailsDTO;
import apps.sarafrika.elimika.tenancy.entity.OrganisationPayoutDetails;
import apps.sarafrika.elimika.tenancy.repository.OrganisationPayoutDetailsRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The M-Pesa number an organisation wants to be paid on, typed in during onboarding. Nothing is sent to verify it:
 * the platform admin who verifies the organisation checks it, and wallet withdrawals do not use it yet.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationPayoutDetailsService {

    private final OrganisationPayoutDetailsRepository repository;
    private final OrganisationRepository organisationRepository;

    @Transactional(readOnly = true)
    public OrganisationPayoutDetailsDTO get(UUID organisationUuid) {
        return repository.findByOrganisationUuid(organisationUuid)
                .map(OrganisationPayoutDetailsService::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("No payout details saved for this organisation"));
    }

    /** Creates or replaces the number. */
    public OrganisationPayoutDetailsDTO save(UUID organisationUuid, OrganisationPayoutDetailsDTO request) {
        if (organisationRepository.findByUuidAndDeletedFalse(organisationUuid).isEmpty()) {
            throw new ResourceNotFoundException("Organisation not found");
        }
        String mpesaNumber = Msisdn.normalise(request.mpesaNumber()).orElseThrow(() ->
                new IllegalArgumentException("Enter a valid M-Pesa number, for example 0712 345 678."));
        OrganisationPayoutDetails details = repository.findByOrganisationUuid(organisationUuid).orElseGet(() -> {
            OrganisationPayoutDetails created = new OrganisationPayoutDetails();
            created.setOrganisationUuid(organisationUuid);
            return created;
        });
        details.setMpesaNumber(mpesaNumber);
        return toDTO(repository.save(details));
    }

    private static OrganisationPayoutDetailsDTO toDTO(OrganisationPayoutDetails details) {
        return new OrganisationPayoutDetailsDTO(details.getOrganisationUuid(), details.getMpesaNumber(),
                details.getLastModifiedDate() != null ? details.getLastModifiedDate() : details.getCreatedDate());
    }
}
