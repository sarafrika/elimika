package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.DifficultyLevelDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface DifficultyLevelService {
    DifficultyLevelDTO createDifficultyLevel(DifficultyLevelDTO difficultyLevelDTO);

    DifficultyLevelDTO getDifficultyLevelByUuid(UUID uuid);

    Page<DifficultyLevelDTO> getAllDifficultyLevels(Pageable pageable);

    DifficultyLevelDTO updateDifficultyLevel(UUID uuid, DifficultyLevelDTO difficultyLevelDTO);

    void deleteDifficultyLevel(UUID uuid);

    Page<DifficultyLevelDTO> search(Map<String, String> searchParams, Pageable pageable);
}
