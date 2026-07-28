package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.OrganisationInvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationInvitationsRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationInvitationsResultDTO;
import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Manages invitations to join an organisation.
 * <p>
 * An invitation is an <em>offer</em>. Sending one provisions nothing: no account is
 * created and no {@code user_organisation_domain_mapping} row exists until the recipient
 * - or, for a minor, their guardian - explicitly accepts.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
public interface OrganisationInvitationService {

    /**
     * Invites one or more recipients into an organisation.
     * <p>
     * Recipients are processed independently so a single bad address cannot cost the
     * sender the rest of the batch; the result reports each one.
     *
     * @param organisationUuid the inviting organisation
     * @param request          recipients, domain, optional classes and message
     * @param inviterUserUuid  the user sending the invitations
     * @return per-recipient outcome
     */
    SendOrganisationInvitationsResultDTO send(UUID organisationUuid,
                                              SendOrganisationInvitationsRequestDTO request,
                                              UUID inviterUserUuid);

    /**
     * Lists an organisation's invitations, newest first.
     *
     * @param organisationUuid the organisation
     * @param statuses         optional status filter; all statuses when null or empty
     */
    List<OrganisationInvitationDTO> listForOrganisation(UUID organisationUuid,
                                                        Collection<InvitationStatus> statuses);

    /**
     * Withdraws a live invitation. Terminal invitations cannot be revoked.
     */
    OrganisationInvitationDTO revoke(UUID organisationUuid, UUID invitationUuid);

    /**
     * Issues a fresh token and expiry for a live invitation and re-sends the email.
     * The previous link stops working.
     */
    OrganisationInvitationDTO resend(UUID organisationUuid, UUID invitationUuid);

    /**
     * Marks live invitations past their expiry as {@link InvitationStatus#EXPIRED}.
     *
     * @return the number of invitations expired
     */
    int expireLapsed();
}
