package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.OrganisationInvitationDTO;
import apps.sarafrika.elimika.tenancy.entity.OrganisationInvitation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrganisationInvitationFactory {

    /**
     * Maps an invitation to its organisation-facing view.
     * <p>
     * Tokens and guardian contact details are deliberately omitted: the organisation
     * supplied nothing about the guardian and has no need to see it back.
     *
     * @param invitation the invitation
     * @param domainName resolved name of the invited domain
     * @param classUuids classes surfaced on acceptance
     */
    public static OrganisationInvitationDTO toDTO(OrganisationInvitation invitation,
                                                  String domainName,
                                                  List<UUID> classUuids) {
        return new OrganisationInvitationDTO(
                invitation.getUuid(),
                invitation.getOrganisationUuid(),
                invitation.getBranchUuid(),
                domainName,
                invitation.getRecipientEmail(),
                invitation.getRecipientName(),
                invitation.getRecipientUserUuid(),
                invitation.getRecipientUserUuid() != null,
                invitation.getInviterUserUuid(),
                invitation.getStatus(),
                invitation.getMessage(),
                classUuids == null ? List.of() : classUuids,
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getDeclinedAt(),
                invitation.getRevokedAt(),
                invitation.isRequiresGuardianConsent(),
                invitation.getGuardianConsentedAt(),
                invitation.getCreatedDate()
        );
    }
}
