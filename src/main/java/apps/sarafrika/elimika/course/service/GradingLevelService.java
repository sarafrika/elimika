package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.GradingLevelDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface GradingLevelService {
    GradingLevelDTO createGradingLevel(GradingLevelDTO gradingLevelDTO);

    GradingLevelDTO getGradingLevelByUuid(UUID uuid);

    Page<GradingLevelDTO> getAllGradingLevels(Pageable pageable);

    GradingLevelDTO updateGradingLevel(UUID uuid, GradingLevelDTO gradingLevelDTO);

    void deleteGradingLevel(UUID uuid);

    Page<GradingLevelDTO> search(Map<String, String> searchParams, Pageable pageable);
}