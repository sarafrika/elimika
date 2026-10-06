package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.dto.AccountStatusDTO;
import apps.sarafrika.elimika.tenancy.dto.DomainApplicationDTO;

import java.util.List;
import java.util.UUID;

/**
 * Owns every write to {@code user_domain_mapping}, so whether a domain is pending or granted is
 * decided in one place.
 */
public interface DomainApprovalService {

    /**
     * Records that the user wants the domain. It is held pending when the domain needs approval
     * and granted otherwise. An existing mapping is left as it is.
     */
    DomainApplicationDTO request(UUID userUuid, UserDomain domain);

    /**
     * Grants the domain outright, for flows that are themselves the approval (a platform admin
     * assigning it, an organisation inviting a member). A pending mapping is approved; a rejected
     * or suspended one is left alone, because only a platform admin may reverse that decision.
     */
    DomainApplicationDTO grant(UUID userUuid, UserDomain domain);

    /** Applies a platform admin's decision to an existing mapping. */
    DomainApplicationDTO decide(UUID userUuid, UserDomain domain, DomainApprovalStatus status,
                                String reason, UUID reviewedBy);

    List<DomainApplicationDTO> applicationsFor(UUID userUuid);

    List<DomainApplicationDTO> applicationsWithStatus(DomainApprovalStatus status, UserDomain domain);

    AccountStatusDTO accountStatus(UUID userUuid);
}
