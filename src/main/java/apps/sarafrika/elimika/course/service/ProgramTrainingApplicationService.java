package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationDTO;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationDecisionRequest;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationRequest;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProgramTrainingApplicationService {

    ProgramTrainingApplicationDTO submitApplication(UUID programUuid, ProgramTrainingApplicationRequest request);

    ProgramTrainingApplicationDTO approveApplication(UUID programUuid,
                                                     UUID applicationUuid,
                                                     ProgramTrainingApplicationDecisionRequest decisionRequest);

    ProgramTrainingApplicationDTO rejectApplication(UUID programUuid,
                                                    UUID applicationUuid,
                                                    ProgramTrainingApplicationDecisionRequest decisionRequest);

    ProgramTrainingApplicationDTO revokeApplication(UUID programUuid,
                                                    UUID applicationUuid,
                                                    ProgramTrainingApplicationDecisionRequest decisionRequest);

    ProgramTrainingApplicationDTO getApplication(UUID programUuid, UUID applicationUuid);

    Page<ProgramTrainingApplicationDTO> getApplications(UUID programUuid,
                                                        Optional<CourseTrainingApplicationStatus> status,
                                                        Pageable pageable);

    Page<ProgramTrainingApplicationDTO> search(Map<String, String> searchParams, Pageable pageable);
}
