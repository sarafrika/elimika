package apps.sarafrika.elimika.course.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface ContentProgressService {
    ContentProgressDTO createContentProgress(ContentProgressDTO contentProgressDTO);
    ContentProgressDTO getContentProgressByUuid(UUID uuid);
    Page<ContentProgressDTO> getAllContentProgresses(Pageable pageable);
    ContentProgressDTO updateContentProgress(UUID uuid, ContentProgressDTO contentProgressDTO);
    void deleteContentProgress(UUID uuid);
    Page<ContentProgressDTO> search(Map<String, String> searchParams, Pageable pageable);
}