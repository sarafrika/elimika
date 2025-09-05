package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.GradingLevelDTO;
import apps.sarafrika.elimika.course.factory.GradingLevelFactory;
import apps.sarafrika.elimika.course.model.GradingLevel;
import apps.sarafrika.elimika.course.repository.GradingLevelRepository;
import apps.sarafrika.elimika.course.service.GradingLevelService;
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
public class GradingLevelServiceImpl implements GradingLevelService {

    private final GradingLevelRepository gradingLevelRepository;
    private final GenericSpecificationBuilder<GradingLevel> specificationBuilder;

    private static final String GRADING_LEVEL_NOT_FOUND_TEMPLATE = "Grading level with ID %s not found";

    @Override
    public GradingLevelDTO createGradingLevel(GradingLevelDTO gradingLevelDTO) {
        GradingLevel gradingLevel = GradingLevelFactory.toEntity(gradingLevelDTO);

        GradingLevel savedGradingLevel = gradingLevelRepository.save(gradingLevel);
        return GradingLevelFactory.toDTO(savedGradingLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public GradingLevelDTO getGradingLevelByUuid(UUID uuid) {
        return gradingLevelRepository.findByUuid(uuid)
                .map(GradingLevelFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(GRADING_LEVEL_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GradingLevelDTO> getAllGradingLevels(Pageable pageable) {
        return gradingLevelRepository.findAll(pageable).map(GradingLevelFactory::toDTO);
    }

    @Override
    public GradingLevelDTO updateGradingLevel(UUID uuid, GradingLevelDTO gradingLevelDTO) {
        GradingLevel existingGradingLevel = gradingLevelRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(GRADING_LEVEL_NOT_FOUND_TEMPLATE, uuid)));

        updateGradingLevelFields(existingGradingLevel, gradingLevelDTO);

        GradingLevel updatedGradingLevel = gradingLevelRepository.save(existingGradingLevel);
        return GradingLevelFactory.toDTO(updatedGradingLevel);
    }

    @Override
    public void deleteGradingLevel(UUID uuid) {
        if (!gradingLevelRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(GRADING_LEVEL_NOT_FOUND_TEMPLATE, uuid));
        }
        gradingLevelRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GradingLevelDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<GradingLevel> spec = specificationBuilder.buildSpecification(
                GradingLevel.class, searchParams);
        return gradingLevelRepository.findAll(spec, pageable).map(GradingLevelFactory::toDTO);
    }

    private void updateGradingLevelFields(GradingLevel existingGradingLevel, GradingLevelDTO dto) {
        if (dto.name() != null) {
            existingGradingLevel.setName(dto.name());
        }
        if (dto.points() != null) {
            existingGradingLevel.setPoints(dto.points());
        }
        if (dto.levelOrder() != null) {
            existingGradingLevel.setLevelOrder(dto.levelOrder());
        }
    }
}