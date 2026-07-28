package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.AcceptInvitationRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.AcceptInvitationResultDTO;
import apps.sarafrika.elimika.tenancy.dto.GuardianConsentRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.GuardianDetailsRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.MyInvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.PublicGuardianInvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.PublicInvitationDTO;

import java.util.List;
import java.util.UUID;

/**
 * The recipient's side of an invitation: reading it, accepting it, declining it, and -
 * where the recipient is a minor - routing it to a guardian.
 * <p>
 * This is the only place an affiliation comes into being from an invitation. Nothing in
 * the sending path writes one.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
public interface InvitationAcceptanceService {

    /**
     * Reads an invitation from its link, without requiring anyone to be signed in.
     * Returns only what the recipient needs in order to decide.
     */
    PublicInvitationDTO lookupByToken(String rawToken);

    /**
     * Accepts an invitation held by the signed-in user.
     * <p>
     * When the accepting user turns out to be below the configured age gate, no
     * affiliation is created: the invitation moves to
     * {@code AWAITING_GUARDIAN_CONSENT} and the caller must supply guardian details next.
     *
     * @param rawToken       token from the emailed link
     * @param request        acknowledgement and, where needed, date of birth
     * @param actingUserUuid the signed-in user, whose email must match the invitation
     */
    AcceptInvitationResultDTO acceptByToken(String rawToken, AcceptInvitationRequestDTO request, UUID actingUserUuid);

    /**
     * Accepts an invitation found in the user's own inbox rather than via an emailed link.
     */
    AcceptInvitationResultDTO acceptByUuid(UUID invitationUuid, AcceptInvitationRequestDTO request, UUID actingUserUuid);

    /**
     * Turns down an invitation. Terminal - the organisation must send a fresh one.
     */
    void declineByToken(String rawToken, UUID actingUserUuid);

    /**
     * Turns down an invitation from the user's own inbox.
     */
    void declineByUuid(UUID invitationUuid, UUID actingUserUuid);

    /**
     * Open invitations addressed to the signed-in user's email address.
     */
    List<MyInvitationDTO> listForUser(UUID userUuid);

    /**
     * Records the guardian a minor nominated, and issues that guardian their own consent
     * link.
     */
    PublicInvitationDTO submitGuardianDetails(String rawToken, GuardianDetailsRequestDTO details, UUID actingUserUuid);

    /**
     * Reads a guardian consent request from its link, without requiring a sign-in.
     */
    PublicGuardianInvitationDTO lookupGuardianRequestByToken(String rawToken);

    /**
     * Records a guardian's consent: creates the child's affiliation and establishes the
     * guardian's own visibility of that child's learning.
     *
     * @param guardianUserUuid the signed-in guardian, whose email must match the nomination
     */
    AcceptInvitationResultDTO guardianConsent(String rawToken, GuardianConsentRequestDTO request, UUID guardianUserUuid);

    /**
     * Records a guardian's refusal. No affiliation is created.
     */
    void guardianDecline(String rawToken, UUID guardianUserUuid);
}
