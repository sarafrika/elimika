package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.AssignmentSubmissionAttachmentDTO;
import apps.sarafrika.elimika.course.factory.AssignmentSubmissionAttachmentFactory;
import apps.sarafrika.elimika.course.model.AssignmentSubmissionAttachment;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionAttachmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionRepository;
import apps.sarafrika.elimika.course.service.AssignmentSubmissionAttachmentService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentSubmissionAttachmentServiceImpl implements AssignmentSubmissionAttachmentService {

    private static final String ATTACHMENT_NOT_FOUND_TEMPLATE = "Assignment submission attachment with ID %s not found";
    private static final String SUBMISSION_NOT_FOUND_TEMPLATE = "Assignment submission with ID %s not found";

    private final AssignmentSubmissionAttachmentRepository attachmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    @Override
    public AssignmentSubmissionAttachmentDTO createAttachment(AssignmentSubmissionAttachmentDTO attachmentDTO) {
        UUID submissionUuid = attachmentDTO.submissionUuid();
        if (!submissionRepository.existsByUuid(submissionUuid)) {
            throw new ResourceNotFoundException(String.format(SUBMISSION_NOT_FOUND_TEMPLATE, submissionUuid));
        }

        AssignmentSubmissionAttachment attachment = AssignmentSubmissionAttachmentFactory.toEntity(attachmentDTO);
        AssignmentSubmissionAttachment saved = attachmentRepository.save(attachment);
        return AssignmentSubmissionAttachmentFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSubmissionAttachmentDTO getAttachmentByUuid(UUID uuid) {
        return attachmentRepository.findByUuid(uuid)
                .map(AssignmentSubmissionAttachmentFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ATTACHMENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionAttachmentDTO> getAttachmentsBySubmission(UUID submissionUuid) {
        return attachmentRepository.findBySubmissionUuid(submissionUuid)
                .stream()
                .map(AssignmentSubmissionAttachmentFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAttachment(UUID uuid) {
        if (!attachmentRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(ATTACHMENT_NOT_FOUND_TEMPLATE, uuid));
        }
        attachmentRepository.deleteByUuid(uuid);
    }
}
