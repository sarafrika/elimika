package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.dto.AccountStatusDTO;
import apps.sarafrika.elimika.tenancy.dto.DomainApplicationDTO;

import java.util.List;
import java.util.UUID;

/** Owns every write to {@code user_domain_mapping}, so pending versus granted is decided in one place. */
public interface DomainApprovalService {

    /** Requests a domain: pending when it needs approval, granted otherwise. An existing mapping is left alone. */
    DomainApplicationDTO request(UUID userUuid, UserDomain domain);

    /** Grants a domain outright (admin assignment, organisation invite); never overrides a rejection or suspension. */
    DomainApplicationDTO grant(UUID userUuid, UserDomain domain);

    /** Applies a platform admin's decision to an existing mapping. */
    DomainApplicationDTO decide(UUID userUuid, UserDomain domain, DomainApprovalStatus status,
                                String reason, UUID reviewedBy);

    List<DomainApplicationDTO> applicationsFor(UUID userUuid);

    List<DomainApplicationDTO> applicationsWithStatus(DomainApprovalStatus status, UserDomain domain);

    AccountStatusDTO accountStatus(UUID userUuid);
}
