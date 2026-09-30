package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRequiredSkillRepository;
import apps.sarafrika.elimika.course.spi.CourseSkillsChangedEvent;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Re-indexes a course's marketplace jobs when the course's skill tags change, since a job with no
 * tags of its own carries its course's skills in {@code required_skill_uuids} and
 * {@code required_skill_names}.
 */
@Component
@RequiredArgsConstructor
class CourseSkillsChangeIndexer {

    private final ClassMarketplaceJobRequiredSkillRepository requiredSkillRepository;
    private final SearchIndexRequests searchIndexRequests;

    @ApplicationModuleListener
    void onCourseSkillsChanged(CourseSkillsChangedEvent event) {
        if (!searchIndexRequests.isEnabled() || event.courseUuid() == null) {
            return;
        }
        searchIndexRequests.enqueue(MarketplaceJobSearchSource.INDEX,
                requiredSkillRepository.findJobUuidsByCourseUuid(event.courseUuid()));
    }
}
