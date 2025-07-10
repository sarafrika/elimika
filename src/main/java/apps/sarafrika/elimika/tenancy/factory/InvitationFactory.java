package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.InvitationDTO;
import apps.sarafrika.elimika.tenancy.entity.Invitation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InvitationFactory {

    public static InvitationDTO toDTO(Invitation invitation) {
        return new InvitationDTO(
                invitation.getUuid(),
                invitation.getToken(),
                invitation.getRecipientEmail(),
                invitation.getRecipientName(),
                invitation.getOrganisationUuid(),
                null, // organisationName - not populated in basic conversion
                invitation.getBranchUuid(),
                null, // branchName - not populated in basic conversion
                invitation.getDomainUuid(),
                null, // domainName - not populated in basic conversion
                invitation.getInviterUuid(),
                invitation.getInviterName(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getDeclinedAt(),
                invitation.getUserUuid(),
                invitation.getNotes(),
                invitation.getCreatedDate(),
                invitation.getLastModifiedDate(),
                invitation.getCreatedBy(),
                invitation.getLastModifiedBy()
        );
    }

    public static Invitation toEntity(InvitationDTO invitationDTO) {
        Invitation invitation = new Invitation();
        invitation.setUuid(invitationDTO.uuid());
        invitation.setToken(invitationDTO.token());
        invitation.setRecipientEmail(invitationDTO.recipientEmail());
        invitation.setRecipientName(invitationDTO.recipientName());
        invitation.setOrganisationUuid(invitationDTO.organisationUuid());
        invitation.setBranchUuid(invitationDTO.branchUuid());
        invitation.setDomainUuid(invitationDTO.domainUuid());
        invitation.setInviterUuid(invitationDTO.inviterUuid());
        invitation.setInviterName(invitationDTO.inviterName());
        invitation.setStatus(invitationDTO.status());
        invitation.setExpiresAt(invitationDTO.expiresAt());
        invitation.setAcceptedAt(invitationDTO.acceptedAt());
        invitation.setDeclinedAt(invitationDTO.declinedAt());
        invitation.setUserUuid(invitationDTO.userUuid());
        invitation.setNotes(invitationDTO.notes());
        invitation.setCreatedBy(invitationDTO.createdBy());
        return invitation;
    }
}