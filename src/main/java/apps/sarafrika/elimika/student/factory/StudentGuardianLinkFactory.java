package apps.sarafrika.elimika.student.factory;

import apps.sarafrika.elimika.student.dto.GuardianStudentLinkDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentSummaryDTO;
import apps.sarafrika.elimika.student.model.StudentGuardianLink;

public final class StudentGuardianLinkFactory {

    private StudentGuardianLinkFactory() {
    }

    public static GuardianStudentLinkDTO toDTO(StudentGuardianLink link,
                                               String studentName,
                                               String guardianDisplayName) {
        return new GuardianStudentLinkDTO(
                link.getUuid(),
                link.getStudentUuid(),
                link.getGuardianUserUuid(),
                studentName,
                guardianDisplayName,
                link.getRelationshipType(),
                link.getShareScope(),
                link.getStatus(),
                link.isPrimaryGuardian(),
                link.getLinkedDate(),
                link.getRevokedDate(),
                link.getNotes()
        );
    }

    public static GuardianStudentSummaryDTO toSummary(StudentGuardianLink link, String studentName) {
        return new GuardianStudentSummaryDTO(
                link.getUuid(),
                link.getStudentUuid(),
                studentName,
                link.getRelationshipType(),
                link.getShareScope(),
                link.getStatus(),
                link.isPrimaryGuardian()
        );
    }
}
