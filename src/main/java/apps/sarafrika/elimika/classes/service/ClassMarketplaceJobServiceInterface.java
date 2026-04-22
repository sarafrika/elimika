package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobAssignmentRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobAssignmentResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDecisionRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequestDTO;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ClassMarketplaceJobServiceInterface {

    ClassMarketplaceJobDTO createJob(ClassMarketplaceJobRequestDTO request);

    ClassMarketplaceJobDTO updateJob(UUID jobUuid, ClassMarketplaceJobRequestDTO request);

    ClassMarketplaceJobDTO getJob(UUID jobUuid);

    Page<ClassMarketplaceJobDTO> listJobs(UUID organisationUuid,
                                          UUID courseUuid,
                                          ClassMarketplaceJobStatus status,
                                          Pageable pageable);

    ClassMarketplaceJobDTO cancelJob(UUID jobUuid);

    ClassMarketplaceJobApplicationDTO applyToJob(UUID jobUuid, ClassMarketplaceJobApplicationRequestDTO request);

    Page<ClassMarketplaceJobApplicationDTO> listJobApplications(UUID jobUuid, Pageable pageable);

    Page<ClassMarketplaceJobApplicationDTO> listMyApplications(Pageable pageable);

    ClassMarketplaceJobApplicationDTO approveApplication(UUID jobUuid,
                                                         UUID applicationUuid,
                                                         ClassMarketplaceJobDecisionRequestDTO request);

    ClassMarketplaceJobApplicationDTO rejectApplication(UUID jobUuid,
                                                        UUID applicationUuid,
                                                        ClassMarketplaceJobDecisionRequestDTO request);

    ClassMarketplaceJobAssignmentResponseDTO assignInstructor(UUID jobUuid,
                                                              ClassMarketplaceJobAssignmentRequestDTO request);
}
