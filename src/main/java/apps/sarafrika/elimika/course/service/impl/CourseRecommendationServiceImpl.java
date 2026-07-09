package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.RecommendedCourseDTO;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseCategoryMapping;
import apps.sarafrika.elimika.course.repository.CourseCategoryMappingRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.service.CourseRecommendationService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Prototype heuristic recommendation engine.
 * <p>
 * "Past courses" = courses the user authored (as a course creator) plus courses they
 * have been approved to train (as an instructor). Candidates are scored by shared
 * category count (weighted) and matching difficulty, excluding courses already taken.
 * When there is no usable history — or nothing overlaps — it falls back to the most
 * recently published courses.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CourseRecommendationServiceImpl implements CourseRecommendationService {

    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 50;
    private static final double CATEGORY_WEIGHT = 3.0;
    private static final double DIFFICULTY_WEIGHT = 1.0;

    private final CourseRepository courseRepository;
    private final CourseCategoryMappingRepository categoryMappingRepository;
    private final CourseTrainingApplicationRepository trainingApplicationRepository;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final InstructorLookupService instructorLookupService;

    @Override
    public List<RecommendedCourseDTO> recommendForUser(UUID userUuid, int limit) {
        final int cappedLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        if (userUuid == null) {
            return popularityFallback(Set.of(), cappedLimit);
        }

        final Set<UUID> pastCourseUuids = resolvePastCourseUuids(userUuid);

        final Set<UUID> profileCategories = new HashSet<>();
        final Set<UUID> profileDifficulties = new HashSet<>();
        if (!pastCourseUuids.isEmpty()) {
            for (Course past : courseRepository.findByUuidIn(new ArrayList<>(pastCourseUuids))) {
                if (past.getDifficultyUuid() != null) {
                    profileDifficulties.add(past.getDifficultyUuid());
                }
                profileCategories.addAll(categoriesOf(past.getUuid()));
            }
        }

        final boolean hasProfile = !profileCategories.isEmpty() || !profileDifficulties.isEmpty();
        if (!hasProfile) {
            return popularityFallback(pastCourseUuids, cappedLimit);
        }

        final List<Scored> scored = new ArrayList<>();
        for (Course candidate : courseRepository.findByStatus(ContentStatus.PUBLISHED)) {
            if (pastCourseUuids.contains(candidate.getUuid())) {
                continue;
            }
            final Set<UUID> candidateCategories = categoriesOf(candidate.getUuid());
            final long sharedCategories = candidateCategories.stream()
                    .filter(profileCategories::contains)
                    .count();
            final boolean sameDifficulty = candidate.getDifficultyUuid() != null
                    && profileDifficulties.contains(candidate.getDifficultyUuid());
            final double score = sharedCategories * CATEGORY_WEIGHT + (sameDifficulty ? DIFFICULTY_WEIGHT : 0.0);
            if (score > 0) {
                scored.add(new Scored(candidate, score, sharedCategories, sameDifficulty));
            }
        }

        if (scored.isEmpty()) {
            return popularityFallback(pastCourseUuids, cappedLimit);
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble((Scored s) -> s.score).reversed()
                        .thenComparing(s -> s.course.getCreatedDate(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(cappedLimit)
                .map(s -> toDto(s.course, s.score, reasonFor(s)))
                .toList();
    }

    private Set<UUID> resolvePastCourseUuids(UUID userUuid) {
        final Set<UUID> past = new HashSet<>();

        courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid)
                .ifPresent(creatorUuid -> past.addAll(courseRepository.findUuidsByCourseCreatorUuid(creatorUuid)));

        instructorLookupService.findInstructorUuidByUserUuid(userUuid).ifPresent(instructorUuid ->
                trainingApplicationRepository
                        .findByApplicantUuidAndStatus(instructorUuid, CourseTrainingApplicationStatus.APPROVED)
                        .forEach(application -> {
                            if (application.getCourseUuid() != null) {
                                past.add(application.getCourseUuid());
                            }
                        }));

        return past;
    }

    private Set<UUID> categoriesOf(UUID courseUuid) {
        final Set<UUID> categories = new HashSet<>();
        for (CourseCategoryMapping mapping : categoryMappingRepository.findByCourseUuid(courseUuid)) {
            if (mapping.getCategoryUuid() != null) {
                categories.add(mapping.getCategoryUuid());
            }
        }
        return categories;
    }

    private List<RecommendedCourseDTO> popularityFallback(Set<UUID> excluded, int limit) {
        return courseRepository.findByStatus(ContentStatus.PUBLISHED).stream()
                .filter(course -> !excluded.contains(course.getUuid()))
                .sorted(Comparator.comparing(Course::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(course -> toDto(course, 0.0, "Popular right now"))
                .toList();
    }

    private String reasonFor(Scored scored) {
        if (scored.sharedCategories > 0) {
            return scored.sharedCategories == 1
                    ? "Shares a topic with your courses"
                    : "Shares " + scored.sharedCategories + " topics with your courses";
        }
        return "Matches the level you usually teach";
    }

    private RecommendedCourseDTO toDto(Course course, double score, String reason) {
        return new RecommendedCourseDTO(
                course.getUuid(),
                course.getName(),
                course.getDescription(),
                course.getThumbnailUrl(),
                reason,
                score
        );
    }

    private record Scored(Course course, double score, long sharedCategories, boolean sameDifficulty) {
    }
}
