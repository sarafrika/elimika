package apps.sarafrika.elimika.tenancy.service;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.OrganisationPayoutDetailsDTO;
import apps.sarafrika.elimika.tenancy.entity.OrganisationPayoutDetails;
import apps.sarafrika.elimika.tenancy.repository.OrganisationPayoutDetailsRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.util.enums.OrganisationPayoutChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * An organisation's M-Pesa Paybill or Till, typed in during onboarding. Nothing is sent to verify it: the platform
 * admin who verifies the organisation checks it, and wallet withdrawals do not use it yet.
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

    /** Creates or replaces the details. */
    public OrganisationPayoutDetailsDTO save(UUID organisationUuid, OrganisationPayoutDetailsDTO request) {
        if (organisationRepository.findByUuidAndDeletedFalse(organisationUuid).isEmpty()) {
            throw new ResourceNotFoundException("Organisation not found");
        }
        String accountNumber = StringUtils.hasText(request.accountNumber()) ? request.accountNumber().trim() : null;
        if (request.channel() == OrganisationPayoutChannel.MPESA_PAYBILL && accountNumber == null) {
            throw new IllegalArgumentException("Enter the account number to credit on the Paybill.");
        }
        if (request.channel() == OrganisationPayoutChannel.MPESA_TILL && accountNumber != null) {
            throw new IllegalArgumentException("A Till number takes no account number.");
        }
        OrganisationPayoutDetails details = repository.findByOrganisationUuid(organisationUuid).orElseGet(() -> {
            OrganisationPayoutDetails created = new OrganisationPayoutDetails();
            created.setOrganisationUuid(organisationUuid);
            return created;
        });
        details.setChannel(request.channel());
        details.setBusinessNumber(request.businessNumber().trim());
        details.setAccountNumber(accountNumber);
        details.setAccountName(request.accountName().trim());
        return toDTO(repository.save(details));
    }

    @Transactional(readOnly = true)
    public boolean hasPayoutDetails(UUID organisationUuid) {
        return repository.existsByOrganisationUuid(organisationUuid);
    }

    private static OrganisationPayoutDetailsDTO toDTO(OrganisationPayoutDetails details) {
        return new OrganisationPayoutDetailsDTO(details.getOrganisationUuid(), details.getChannel(),
                details.getBusinessNumber(), details.getAccountNumber(), details.getAccountName(),
                details.getLastModifiedDate() != null ? details.getLastModifiedDate() : details.getCreatedDate());
    }
}
