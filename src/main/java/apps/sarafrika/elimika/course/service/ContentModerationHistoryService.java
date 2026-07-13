package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ContentModerationHistoryDTO;
import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import apps.sarafrika.elimika.course.util.enums.ModerationContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ContentModerationHistoryService {

    void record(ModerationContentType contentType, UUID contentUuid, ModerationAction action, String reason);

    Page<ContentModerationHistoryDTO> getHistory(ModerationContentType contentType, UUID contentUuid, Pageable pageable);
}
