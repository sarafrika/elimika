package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.ContentModerationHistoryDTO;
import apps.sarafrika.elimika.course.factory.ContentModerationHistoryFactory;
import apps.sarafrika.elimika.course.model.ContentModerationHistory;
import apps.sarafrika.elimika.course.repository.ContentModerationHistoryRepository;
import apps.sarafrika.elimika.course.service.ContentModerationHistoryService;
import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import apps.sarafrika.elimika.course.util.enums.ModerationContentType;
import apps.sarafrika.elimika.shared.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContentModerationHistoryServiceImpl implements ContentModerationHistoryService {

    private final ContentModerationHistoryRepository contentModerationHistoryRepository;
    private final UserContextService userContextService;

    @Override
    public void record(ModerationContentType contentType, UUID contentUuid, ModerationAction action, String reason) {
        ContentModerationHistory entry = new ContentModerationHistory();
        entry.setContentType(contentType);
        entry.setContentUuid(contentUuid);
        entry.setAction(action);
        entry.setReason(reason);
        userContextService.getCurrentUserUuidOptional().ifPresent(entry::setModeratorUuid);

        contentModerationHistoryRepository.save(entry);
        log.info("Recorded moderation action {} for {} {}", action, contentType, contentUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentModerationHistoryDTO> getHistory(ModerationContentType contentType, UUID contentUuid, Pageable pageable) {
        return contentModerationHistoryRepository
                .findByContentTypeAndContentUuidOrderByCreatedDateDesc(contentType, contentUuid, pageable)
                .map(ContentModerationHistoryFactory::toDTO);
    }
}
