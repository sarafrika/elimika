package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.commerce.internal.config.InternalCommerceProperties;
import apps.sarafrika.elimika.commerce.internal.service.CatalogueBackfillService;
import apps.sarafrika.elimika.commerce.internal.service.CatalogueProvisioningService;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogueBackfillServiceImpl implements CatalogueBackfillService {

    private final InternalCommerceProperties internalCommerceProperties;
    private final CourseRepository courseRepository;
    private final ClassDefinitionRepository classDefinitionRepository;
    private final CatalogueProvisioningService catalogueProvisioningService;

    @Override
    public int backfillPublishedCatalogue() {
        if (!Boolean.TRUE.equals(internalCommerceProperties.getEnabled())) {
            log.info("Internal commerce disabled; skipping catalogue backfill");
            return 0;
        }

        List<Course> publishedCourses = courseRepository.findByStatus(ContentStatus.PUBLISHED);
        int processedClasses = 0;

        for (Course course : publishedCourses) {
            if (course.getUuid() == null) {
                continue;
            }
            var classDefinitions = classDefinitionRepository.findByCourseUuid(course.getUuid());
            for (var definition : classDefinitions) {
                UUID classUuid = definition.getUuid();
                if (classUuid == null) {
                    continue;
                }
                catalogueProvisioningService.ensureClassIsPurchasable(classUuid);
                processedClasses++;
            }
        }

        log.info("Catalogue backfill finished for {} published courses; processed {} class definitions",
                publishedCourses.size(), processedClasses);
        return processedClasses;
    }
}
