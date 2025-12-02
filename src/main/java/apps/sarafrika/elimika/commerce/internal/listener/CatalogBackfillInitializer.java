package apps.sarafrika.elimika.commerce.internal.listener;

import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.commerce.internal.config.InternalCommerceProperties;
import apps.sarafrika.elimika.commerce.internal.service.CatalogProvisioningService;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Backfills catalog entries for already published courses/classes on startup.
 * This is idempotent and only runs when internal commerce is enabled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogBackfillInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final InternalCommerceProperties internalCommerceProperties;
    private final CourseRepository courseRepository;
    private final ClassDefinitionRepository classDefinitionRepository;
    private final CatalogProvisioningService catalogProvisioningService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!Boolean.TRUE.equals(internalCommerceProperties.getEnabled())) {
            log.info("Internal commerce disabled; skipping catalog backfill");
            return;
        }

        List<Course> publishedCourses = courseRepository.findByStatus(ContentStatus.PUBLISHED);
        int processedClasses = 0;

        for (Course course : publishedCourses) {
            if (course.getUuid() == null) {
                continue;
            }
            List<apps.sarafrika.elimika.classes.model.ClassDefinition> classDefinitions =
                    classDefinitionRepository.findByCourseUuid(course.getUuid());
            for (apps.sarafrika.elimika.classes.model.ClassDefinition definition : classDefinitions) {
                UUID classUuid = definition.getUuid();
                if (classUuid == null) {
                    continue;
                }
                catalogProvisioningService.ensureClassIsPurchasable(classUuid);
                processedClasses++;
            }
        }

        log.info("Catalog backfill finished for {} published courses; processed {} class definitions",
                publishedCourses.size(), processedClasses);
    }
}
