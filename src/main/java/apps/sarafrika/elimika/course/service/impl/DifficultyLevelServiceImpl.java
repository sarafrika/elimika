package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.DifficultyLevelDTO;
import apps.sarafrika.elimika.course.factory.DifficultyLevelFactory;
import apps.sarafrika.elimika.course.model.DifficultyLevel;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.DifficultyLevelRepository;
import apps.sarafrika.elimika.course.service.DifficultyLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DifficultyLevelServiceImpl implements DifficultyLevelService {

    private final DifficultyLevelRepository difficultyLevelRepository;
    private final CourseRepository courseRepository;

    private final GenericSpecificationBuilder<DifficultyLevel> specificationBuilder;

    private static final String DIFFICULTY_LEVEL_NOT_FOUND_TEMPLATE = "Difficulty level with ID %s not found";

    @Override
    public DifficultyLevelDTO createDifficultyLevel(DifficultyLevelDTO difficultyLevelDTO) {
        DifficultyLevel difficultyLevel = DifficultyLevelFactory.toEntity(difficultyLevelDTO);

        DifficultyLevel savedDifficultyLevel = difficultyLevelRepository.save(difficultyLevel);
        return DifficultyLevelFactory.toDTO(savedDifficultyLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public DifficultyLevelDTO getDifficultyLevelByUuid(UUID uuid) {
        return difficultyLevelRepository.findByUuid(uuid)
                .map(DifficultyLevelFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(DIFFICULTY_LEVEL_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DifficultyLevelDTO> getAllDifficultyLevels(Pageable pageable) {
        return difficultyLevelRepository.findAll(pageable).map(DifficultyLevelFactory::toDTO);
    }

    @Override
    public DifficultyLevelDTO updateDifficultyLevel(UUID uuid, DifficultyLevelDTO difficultyLevelDTO) {
        DifficultyLevel existingDifficultyLevel = difficultyLevelRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(DIFFICULTY_LEVEL_NOT_FOUND_TEMPLATE, uuid)));

        updateDifficultyLevelFields(existingDifficultyLevel, difficultyLevelDTO);

        DifficultyLevel updatedDifficultyLevel = difficultyLevelRepository.save(existingDifficultyLevel);
        return DifficultyLevelFactory.toDTO(updatedDifficultyLevel);
    }

    @Override
    public void deleteDifficultyLevel(UUID uuid) {
        if (!difficultyLevelRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(DIFFICULTY_LEVEL_NOT_FOUND_TEMPLATE, uuid));
        }
        difficultyLevelRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DifficultyLevelDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<DifficultyLevel> spec = specificationBuilder.buildSpecification(
                DifficultyLevel.class, searchParams);
        return difficultyLevelRepository.findAll(spec, pageable).map(DifficultyLevelFactory::toDTO);
    }

    // Domain-specific methods leveraging DifficultyLevelDTO computed properties
    @Transactional(readOnly = true)
    public List<DifficultyLevelDTO> getAllDifficultyLevelsInOrder() {
        return difficultyLevelRepository.findAllByOrderByLevelOrderAsc()
                .stream()
                .map(DifficultyLevelFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DifficultyLevelDTO getEntryLevel() {
        return difficultyLevelRepository.findByLevelOrder(1)
                .map(DifficultyLevelFactory::toDTO)
                .filter(DifficultyLevelDTO::isEntryLevel) // Using computed property
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public DifficultyLevelDTO getNextLevel(UUID currentLevelUuid) {
        DifficultyLevel currentLevel = difficultyLevelRepository.findByUuid(currentLevelUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(DIFFICULTY_LEVEL_NOT_FOUND_TEMPLATE, currentLevelUuid)));

        return difficultyLevelRepository.findByLevelOrder(currentLevel.getLevelOrder() + 1)
                .map(DifficultyLevelFactory::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public DifficultyLevelDTO getPreviousLevel(UUID currentLevelUuid) {
        DifficultyLevel currentLevel = difficultyLevelRepository.findByUuid(currentLevelUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(DIFFICULTY_LEVEL_NOT_FOUND_TEMPLATE, currentLevelUuid)));

        if (currentLevel.getLevelOrder() <= 1) {
            return null; // No previous level for entry level
        }

        return difficultyLevelRepository.findByLevelOrder(currentLevel.getLevelOrder() - 1)
                .map(DifficultyLevelFactory::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean canDeleteLevel(UUID difficultyLevelUuid) {
        // Check if any courses are using this difficulty level
        return courseRepository.countByDifficultyUuid(difficultyLevelUuid) == 0;
    }

    public void reorderDifficultyLevels(List<UUID> levelUuids) {
        for (int i = 0; i < levelUuids.size(); i++) {
            int finalI = i;
            DifficultyLevel level = difficultyLevelRepository.findByUuid(levelUuids.get(i))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(DIFFICULTY_LEVEL_NOT_FOUND_TEMPLATE, levelUuids.get(finalI))));

            level.setLevelOrder(i + 1);
            difficultyLevelRepository.save(level);
        }
    }

    private void updateDifficultyLevelFields(DifficultyLevel existingDifficultyLevel, DifficultyLevelDTO dto) {
        if (dto.name() != null) {
            existingDifficultyLevel.setName(dto.name());
        }
        if (dto.levelOrder() != null) {
            existingDifficultyLevel.setLevelOrder(dto.levelOrder());
        }
        if (dto.description() != null) {
            existingDifficultyLevel.setDescription(dto.description());
        }
    }
}