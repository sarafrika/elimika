package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ContentModerationHistory;
import apps.sarafrika.elimika.course.util.enums.ModerationContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContentModerationHistoryRepository extends JpaRepository<ContentModerationHistory, Long> {

    Page<ContentModerationHistory> findByContentTypeAndContentUuidOrderByCreatedDateDesc(
            ModerationContentType contentType, UUID contentUuid, Pageable pageable);
}
