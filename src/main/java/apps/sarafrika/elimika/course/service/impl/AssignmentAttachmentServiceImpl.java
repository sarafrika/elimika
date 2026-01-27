package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.AssignmentAttachmentDTO;
import apps.sarafrika.elimika.course.factory.AssignmentAttachmentFactory;
import apps.sarafrika.elimika.course.model.AssignmentAttachment;
import apps.sarafrika.elimika.course.repository.AssignmentAttachmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.service.AssignmentAttachmentService;
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
public class AssignmentAttachmentServiceImpl implements AssignmentAttachmentService {

    private static final String ATTACHMENT_NOT_FOUND_TEMPLATE = "Assignment attachment with ID %s not found";
    private static final String ASSIGNMENT_NOT_FOUND_TEMPLATE = "Assignment with ID %s not found";

    private final AssignmentAttachmentRepository attachmentRepository;
    private final AssignmentRepository assignmentRepository;

    @Override
    public AssignmentAttachmentDTO createAttachment(AssignmentAttachmentDTO attachmentDTO) {
        UUID assignmentUuid = attachmentDTO.assignmentUuid();
        if (!assignmentRepository.existsByUuid(assignmentUuid)) {
            throw new ResourceNotFoundException(String.format(ASSIGNMENT_NOT_FOUND_TEMPLATE, assignmentUuid));
        }

        AssignmentAttachment attachment = AssignmentAttachmentFactory.toEntity(attachmentDTO);
        AssignmentAttachment saved = attachmentRepository.save(attachment);
        return AssignmentAttachmentFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentAttachmentDTO getAttachmentByUuid(UUID uuid) {
        return attachmentRepository.findByUuid(uuid)
                .map(AssignmentAttachmentFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ATTACHMENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentAttachmentDTO> getAttachmentsByAssignment(UUID assignmentUuid) {
        return attachmentRepository.findByAssignmentUuid(assignmentUuid)
                .stream()
                .map(AssignmentAttachmentFactory::toDTO)
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
