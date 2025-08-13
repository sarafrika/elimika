package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.RubricCriteriaDTO;
import apps.sarafrika.elimika.course.factory.RubricCriteriaFactory;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.repository.RubricCriteriaRepository;
import apps.sarafrika.elimika.course.service.RubricCriteriaService;
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
public class RubricCriteriaServiceImpl implements RubricCriteriaService {

    private final RubricCriteriaRepository rubricCriteriaRepository;
    private final GenericSpecificationBuilder<RubricCriteria> specificationBuilder;

    private static final String RUBRIC_CRITERIA_NOT_FOUND_TEMPLATE = "Rubric criteria with ID %s not found";

    @Override
    public RubricCriteriaDTO createRubricCriteria(RubricCriteriaDTO rubricCriteriaDTO) {
        RubricCriteria rubricCriteria = RubricCriteriaFactory.toEntity(rubricCriteriaDTO);

        RubricCriteria savedRubricCriteria = rubricCriteriaRepository.save(rubricCriteria);
        return RubricCriteriaFactory.toDTO(savedRubricCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public RubricCriteriaDTO getRubricCriteriaByUuid(UUID uuid) {
        return rubricCriteriaRepository.findByUuid(uuid)
                .map(RubricCriteriaFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(RUBRIC_CRITERIA_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricCriteriaDTO> getAllRubricCriterias(Pageable pageable) {
        return rubricCriteriaRepository.findAll(pageable).map(RubricCriteriaFactory::toDTO);
    }

    @Override
    public RubricCriteriaDTO updateRubricCriteria(UUID uuid, RubricCriteriaDTO rubricCriteriaDTO) {
        RubricCriteria existingRubricCriteria = rubricCriteriaRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(RUBRIC_CRITERIA_NOT_FOUND_TEMPLATE, uuid)));

        updateRubricCriteriaFields(existingRubricCriteria, rubricCriteriaDTO);

        RubricCriteria updatedRubricCriteria = rubricCriteriaRepository.save(existingRubricCriteria);
        return RubricCriteriaFactory.toDTO(updatedRubricCriteria);
    }

    @Override
    public void deleteRubricCriteria(UUID uuid) {
        if (!rubricCriteriaRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(RUBRIC_CRITERIA_NOT_FOUND_TEMPLATE, uuid));
        }
        rubricCriteriaRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricCriteriaDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<RubricCriteria> spec = specificationBuilder.buildSpecification(
                RubricCriteria.class, searchParams);
        return rubricCriteriaRepository.findAll(spec, pageable).map(RubricCriteriaFactory::toDTO);
    }

    private void updateRubricCriteriaFields(RubricCriteria existingRubricCriteria, RubricCriteriaDTO dto) {
        if (dto.rubricUuid() != null) {
            existingRubricCriteria.setRubricUuid(dto.rubricUuid());
        }
        if (dto.componentName() != null) {
            existingRubricCriteria.setComponentName(dto.componentName());
        }
        if (dto.description() != null) {
            existingRubricCriteria.setDescription(dto.description());
        }
        if (dto.displayOrder() != null) {
            existingRubricCriteria.setDisplayOrder(dto.displayOrder());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricCriteriaDTO> getAllByRubricUuid(UUID rubricUuid, Pageable pageable) {
        return rubricCriteriaRepository.findAllByRubricUuid(rubricUuid, pageable).map(RubricCriteriaFactory::toDTO);
    }
}