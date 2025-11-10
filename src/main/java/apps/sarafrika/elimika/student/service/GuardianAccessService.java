package apps.sarafrika.elimika.student.service;

import apps.sarafrika.elimika.student.dto.*;

import java.util.List;
import java.util.UUID;

public interface GuardianAccessService {

    GuardianStudentLinkDTO createOrUpdateLink(GuardianStudentLinkRequest request, UUID actorUuid);

    void revokeLink(UUID linkUuid, UUID actorUuid, String reason);

    List<GuardianStudentSummaryDTO> getGuardianStudentSummaries(UUID guardianUserUuid);

    GuardianStudentDashboardDTO getGuardianDashboard(UUID guardianUserUuid, UUID studentUuid);
}
