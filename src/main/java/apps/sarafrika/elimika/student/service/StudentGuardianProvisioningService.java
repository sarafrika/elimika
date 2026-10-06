package apps.sarafrika.elimika.student.service;

import apps.sarafrika.elimika.student.dto.GuardianInvitationRegistrationRequestDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkDTO;
import apps.sarafrika.elimika.student.dto.MyStudentGuardianInvitationDTO;
import apps.sarafrika.elimika.student.dto.PublicStudentGuardianInvitationDTO;
import apps.sarafrika.elimika.student.dto.StudentGuardianDTO;
import apps.sarafrika.elimika.student.dto.StudentGuardianRequestDTO;
import apps.sarafrika.elimika.student.model.Student;

import java.util.List;
import java.util.UUID;

/**
 * Turns the guardians a student names into access: known accounts are linked at once (granting
 * {@code parent}), unknown emails are invited, and removed guardians lose any pending invitation.
 */
public interface StudentGuardianProvisioningService {

    /** Reconciles the student's guardians with {@code guardians}; null leaves them untouched. */
    void syncGuardians(Student student, List<StudentGuardianRequestDTO> guardians, UUID actorUuid);

    List<StudentGuardianDTO> getGuardians(UUID studentUuid);

    StudentGuardianDTO resendInvitation(UUID studentUuid, UUID guardianUuid, UUID actorUuid);

    PublicStudentGuardianInvitationDTO lookupByToken(String rawToken);

    GuardianStudentLinkDTO acceptByToken(String rawToken, UUID guardianUserUuid);

    void declineByToken(String rawToken);

    /** Creates the invited guardian's account for the invited email, then links them. */
    GuardianStudentLinkDTO registerAndAccept(String rawToken, GuardianInvitationRegistrationRequestDTO request);

    List<MyStudentGuardianInvitationDTO> listOpenInvitationsFor(UUID userUuid);

    GuardianStudentLinkDTO acceptByUuid(UUID invitationUuid, UUID guardianUserUuid);

    void declineByUuid(UUID invitationUuid, UUID guardianUserUuid);
}
