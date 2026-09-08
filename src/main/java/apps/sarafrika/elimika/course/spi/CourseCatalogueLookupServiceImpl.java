package apps.sarafrika.elimika.course.spi;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseCategoryMappingRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.spi.CourseCatalogueLookupService;
import apps.sarafrika.elimika.shared.spi.CourseCatalogueSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Public course attributes for a whole page of catalogue rows, in three queries.
 * <p>
 * One for the courses, one for their categories, one for their creators' names — regardless of how
 * many rows the page holds. The shape matters more than the saving: the caller cannot accidentally
 * reintroduce a per-row lookup, because there is nothing here to call per row.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCatalogueLookupServiceImpl implements CourseCatalogueLookupService {

    private final CourseRepository courseRepository;
    private final CourseCategoryMappingRepository categoryMappingRepository;
    private final CourseCreatorLookupService courseCreatorLookupService;

    @Override
    public Map<UUID, CourseCatalogueSnapshot> findPublicByUuids(Collection<UUID> courseUuids) {
        if (courseUuids == null || courseUuids.isEmpty()) {
            return Map.of();
        }

        List<UUID> distinct = courseUuids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }

        List<Course> courses = courseRepository.findByUuidIn(distinct);
        if (courses.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<String>> categories = categoriesFor(courses);
        Map<UUID, String> creatorNames = creatorNamesFor(courses);

        Map<UUID, CourseCatalogueSnapshot> snapshots = new LinkedHashMap<>();
        for (Course course : courses) {
            snapshots.put(course.getUuid(), new CourseCatalogueSnapshot(
                    course.getUuid(),
                    course.getName(),
                    course.getDescription(),
                    course.getThumbnailUrl(),
                    course.getDurationHours(),
                    course.getDurationMinutes(),
                    categories.getOrDefault(course.getUuid(), List.of()),
                    course.getPrice(),
                    course.getAgeLowerLimit(),
                    course.getAgeUpperLimit(),
                    isPublished(course),
                    acceptsNewEnrollments(course),
                    course.getCourseCreatorUuid(),
                    creatorNames.get(course.getCourseCreatorUuid())
            ));
        }
        return snapshots;
    }

    private Map<UUID, List<String>> categoriesFor(List<Course> courses) {
        List<UUID> uuids = courses.stream().map(Course::getUuid).toList();
        Map<UUID, List<String>> byCourse = new HashMap<>();
        for (Object[] row : categoryMappingRepository.findCategoryNamesByCourseUuidIn(uuids)) {
            UUID courseUuid = (UUID) row[0];
            String name = (String) row[1];
            byCourse.computeIfAbsent(courseUuid, key -> new ArrayList<>()).add(name);
        }
        return byCourse;
    }

    private Map<UUID, String> creatorNamesFor(List<Course> courses) {
        Set<UUID> creatorUuids = new HashSet<>();
        for (Course course : courses) {
            if (course.getCourseCreatorUuid() != null) {
                creatorUuids.add(course.getCourseCreatorUuid());
            }
        }
        return creatorUuids.isEmpty() ? Map.of() : courseCreatorLookupService.findFullNamesByUuids(creatorUuids);
    }

    /** Mirrors {@code CourseDTO.isPublished()} so the catalogue and the course page agree. */
    private static boolean isPublished(Course course) {
        return course.getStatus() == ContentStatus.PUBLISHED;
    }

    /** Mirrors {@code CourseDTO.acceptsNewEnrollments()}. */
    private static boolean acceptsNewEnrollments(Course course) {
        return Boolean.TRUE.equals(course.getActive())
                && Boolean.TRUE.equals(course.getAdminApproved())
                && (isPublished(course) || course.getStatus() == ContentStatus.DRAFT);
    }
}
