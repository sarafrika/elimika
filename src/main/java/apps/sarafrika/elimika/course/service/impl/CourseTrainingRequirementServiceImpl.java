package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingRequirementDTO;
import apps.sarafrika.elimika.course.factory.CourseTrainingRequirementFactory;
import apps.sarafrika.elimika.course.model.CourseTrainingRequirement;
import apps.sarafrika.elimika.course.repository.CourseTrainingRequirementRepository;
import apps.sarafrika.elimika.course.service.CourseTrainingRequirementService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseTrainingRequirementServiceImpl implements CourseTrainingRequirementService {

    private final CourseTrainingRequirementRepository repository;
    private final GenericSpecificationBuilder<CourseTrainingRequirement> specificationBuilder;

    private static final String NOT_FOUND_TEMPLATE = "Course training requirement with UUID %s not found";

    @Override
    public CourseTrainingRequirementDTO create(UUID courseUuid, CourseTrainingRequirementDTO dto) {
        CourseTrainingRequirement entity = CourseTrainingRequirementFactory.toEntity(dto);
        entity.setCourseUuid(courseUuid);
        if (entity.getIsMandatory() == null) {
            entity.setIsMandatory(true);
        }
        CourseTrainingRequirement saved = repository.save(entity);
        return CourseTrainingRequirementFactory.toDTO(saved);
    }

    @Override
    public CourseTrainingRequirementDTO update(UUID courseUuid, UUID requirementUuid, CourseTrainingRequirementDTO dto) {
        CourseTrainingRequirement existing = repository.findByUuid(requirementUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND_TEMPLATE, requirementUuid)));

        if (!existing.getCourseUuid().equals(courseUuid)) {
            throw new ResourceNotFoundException(String.format("Requirement %s does not belong to course %s", requirementUuid, courseUuid));
        }

        existing.setCourseUuid(courseUuid);
        if (dto.requirementType() != null) {
            existing.setRequirementType(dto.requirementType());
        }
        if (dto.name() != null) {
            existing.setName(dto.name());
        }
        if (dto.description() != null) {
            existing.setDescription(dto.description());
        }
        if (dto.quantity() != null) {
            existing.setQuantity(dto.quantity());
        }
        if (dto.unit() != null) {
            existing.setUnit(dto.unit());
        }
        if (dto.providedBy() != null) {
            existing.setProvidedBy(dto.providedBy());
        }
        if (dto.isMandatory() != null) {
            existing.setIsMandatory(dto.isMandatory());
        }

        CourseTrainingRequirement updated = repository.save(existing);
        return CourseTrainingRequirementFactory.toDTO(updated);
    }

    @Override
    public void delete(UUID courseUuid, UUID requirementUuid) {
        CourseTrainingRequirement existing = repository.findByUuid(requirementUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND_TEMPLATE, requirementUuid)));

        if (!existing.getCourseUuid().equals(courseUuid)) {
            throw new ResourceNotFoundException(String.format("Requirement %s does not belong to course %s", requirementUuid, courseUuid));
        }
        repository.deleteByUuid(requirementUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseTrainingRequirementDTO getByUuid(UUID courseUuid, UUID requirementUuid) {
        CourseTrainingRequirement requirement = repository.findByUuid(requirementUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND_TEMPLATE, requirementUuid)));

        if (!requirement.getCourseUuid().equals(courseUuid)) {
            throw new ResourceNotFoundException(String.format("Requirement %s does not belong to course %s", requirementUuid, courseUuid));
        }

        return CourseTrainingRequirementFactory.toDTO(requirement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseTrainingRequirementDTO> findByCourseUuid(UUID courseUuid) {
        return repository.findByCourseUuid(courseUuid).stream()
                .map(CourseTrainingRequirementFactory::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseTrainingRequirementDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseTrainingRequirement> spec = specificationBuilder.buildSpecification(CourseTrainingRequirement.class, searchParams);
        return repository.findAll(spec, pageable).map(CourseTrainingRequirementFactory::toDTO);
    }
}
