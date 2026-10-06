package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.dto.AccountStatusDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminDomainApplicationDTO;
import apps.sarafrika.elimika.tenancy.dto.DomainApplicationDTO;

import java.util.List;
import java.util.Optional;
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

    /** Admin moderation of a domain without its own profile review; action is approve, reject or revoke. */
    DomainApplicationDTO moderate(UUID userUuid, UserDomain domain, String action, String reason, UUID reviewedBy);

    /** Approves the domain because its profile review passed (e.g. an instructor was verified). */
    void approveReviewedProfile(UUID userUuid, UserDomain domain, UUID reviewedBy);

    /** Approves the organisation_user domain of every admin of an approved organisation. */
    void approveOrganisationAdmins(UUID organisationUuid, UUID reviewedBy);

    /** The admin approval queue, oldest first; {@code submitted} narrows to submitted (true) or unsubmitted (false). */
    List<AdminDomainApplicationDTO> queue(DomainApprovalStatus status, UserDomain domain, Boolean submitted);

    /** Records an onboarding submission: back to PENDING with submitted_at, admins told; no-review domains just stamp it. */
    DomainApplicationDTO recordSubmission(UUID userUuid, UserDomain domain);

    /** Stamps submitted_at on the pending organisation_user domain of every admin of the organisation. */
    void markOrganisationAdminsSubmitted(UUID organisationUuid);

    Optional<DomainApplicationDTO> application(UUID userUuid, UserDomain domain);

    boolean requiresApproval(UserDomain domain);

    long countWithStatus(DomainApprovalStatus status);

    List<DomainApplicationDTO> applicationsFor(UUID userUuid);

    AccountStatusDTO accountStatus(UUID userUuid);
}
