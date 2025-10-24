package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDTO;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDecisionRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationRequest;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CourseTrainingApplicationService {

    CourseTrainingApplicationDTO submitApplication(UUID courseUuid, CourseTrainingApplicationRequest request);

    CourseTrainingApplicationDTO approveApplication(UUID courseUuid,
                                                    UUID applicationUuid,
                                                    CourseTrainingApplicationDecisionRequest decisionRequest);

    CourseTrainingApplicationDTO rejectApplication(UUID courseUuid,
                                                   UUID applicationUuid,
                                                   CourseTrainingApplicationDecisionRequest decisionRequest);

    CourseTrainingApplicationDTO revokeApplication(UUID courseUuid,
                                                   UUID applicationUuid,
                                                   CourseTrainingApplicationDecisionRequest decisionRequest);

    CourseTrainingApplicationDTO getApplication(UUID courseUuid, UUID applicationUuid);

    Page<CourseTrainingApplicationDTO> getApplications(UUID courseUuid,
                                                       Optional<CourseTrainingApplicationStatus> status,
                                                       Pageable pageable);

    Page<CourseTrainingApplicationDTO> search(Map<String, String> searchParams, Pageable pageable);

    boolean hasApprovedApplication(UUID courseUuid, CourseTrainingApplicantType applicantType, UUID applicantUuid);
}
