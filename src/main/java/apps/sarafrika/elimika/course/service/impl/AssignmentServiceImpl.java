package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.AssignmentDTO;
import apps.sarafrika.elimika.course.factory.AssignmentFactory;
import apps.sarafrika.elimika.course.internal.AssignmentMediaValidationService;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final GenericSpecificationBuilder<Assignment> specificationBuilder;
    private final AssignmentMediaValidationService assignmentMediaValidationService;

    private static final String ASSIGNMENT_NOT_FOUND_TEMPLATE = "Assignment with ID %s not found";

    @Override
    public AssignmentDTO createAssignment(AssignmentDTO assignmentDTO) {
        Assignment assignment = AssignmentFactory.toEntity(assignmentDTO);

        String[] normalizedSubmissionTypes = assignmentMediaValidationService.normalizeSubmissionTypes(assignment.getSubmissionTypes());
        assignmentMediaValidationService.validateSubmissionTypes(normalizedSubmissionTypes);
        assignment.setSubmissionTypes(normalizedSubmissionTypes);

        if (assignment.getIsPublished() == null) {
            assignment.setIsPublished(false);
        }

        Assignment savedAssignment = assignmentRepository.save(assignment);
        return AssignmentFactory.toDTO(savedAssignment);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDTO getAssignmentByUuid(UUID uuid) {
        return assignmentRepository.findByUuid(uuid)
                .map(AssignmentFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSIGNMENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentDTO> getAllAssignments(Pageable pageable) {
        return assignmentRepository.findAll(pageable).map(AssignmentFactory::toDTO);
    }

    @Override
    public AssignmentDTO updateAssignment(UUID uuid, AssignmentDTO assignmentDTO) {
        Assignment existingAssignment = assignmentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSIGNMENT_NOT_FOUND_TEMPLATE, uuid)));

        updateAssignmentFields(existingAssignment, assignmentDTO);

        Assignment updatedAssignment = assignmentRepository.save(existingAssignment);
        return AssignmentFactory.toDTO(updatedAssignment);
    }

    @Override
    public void deleteAssignment(UUID uuid) {
        if (!assignmentRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(ASSIGNMENT_NOT_FOUND_TEMPLATE, uuid));
        }
        assignmentRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Assignment> spec = specificationBuilder.buildSpecification(
                Assignment.class, searchParams);
        return assignmentRepository.findAll(spec, pageable).map(AssignmentFactory::toDTO);
    }

    private void updateAssignmentFields(Assignment existingAssignment, AssignmentDTO dto) {
        if (dto.lessonUuid() != null) {
            existingAssignment.setLessonUuid(dto.lessonUuid());
        }
        if (dto.scope() != null) {
            existingAssignment.setScope(dto.scope());
        }
        if (dto.classDefinitionUuid() != null) {
            existingAssignment.setClassDefinitionUuid(dto.classDefinitionUuid());
        }
        if (dto.sourceAssignmentUuid() != null) {
            existingAssignment.setSourceAssignmentUuid(dto.sourceAssignmentUuid());
        }
        if (dto.title() != null) {
            existingAssignment.setTitle(dto.title());
        }
        if (dto.description() != null) {
            existingAssignment.setDescription(dto.description());
        }
        if (dto.instructions() != null) {
            existingAssignment.setInstructions(dto.instructions());
        }
        if (dto.dueDate() != null) {
            existingAssignment.setDueDate(dto.dueDate());
        }
        if (dto.maxPoints() != null) {
            existingAssignment.setMaxPoints(dto.maxPoints());
        }
        if (dto.rubricUuid() != null) {
            existingAssignment.setRubricUuid(dto.rubricUuid());
        }
        if (dto.submissionTypes() != null) {
            String[] normalizedSubmissionTypes = assignmentMediaValidationService.normalizeSubmissionTypes(dto.submissionTypes());
            assignmentMediaValidationService.validateSubmissionTypes(normalizedSubmissionTypes);
            existingAssignment.setSubmissionTypes(normalizedSubmissionTypes);
        }
        if (dto.published() != null) {
            existingAssignment.setIsPublished(dto.published());
        }
    }
}
