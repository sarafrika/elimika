package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseReviewDTO;
import apps.sarafrika.elimika.course.dto.LessonContentDTO;
import apps.sarafrika.elimika.course.dto.OrganisationCourseContentDTO;
import apps.sarafrika.elimika.course.dto.OrganisationCourseLessonDTO;
import apps.sarafrika.elimika.course.internal.security.CourseContentAccessResolver;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.dto.PublicCourseProfileDTO;
import apps.sarafrika.elimika.course.repository.CourseCategoryMappingRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingRequirementRepository;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.course.service.CourseContentService;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import apps.sarafrika.elimika.course.service.CourseReviewService;
import apps.sarafrika.elimika.course.service.LessonContentService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.CourseContentAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseContentServiceImpl implements CourseContentService {

    private final CourseRepository courseRepository;
    private final CourseCategoryMappingRepository mappingRepository;
    private final CourseTrainingRequirementRepository trainingRequirementRepository;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final LessonRepository lessonRepository;
    private final LessonContentService lessonContentService;
    private final CourseReviewService courseReviewService;
    private final CourseContentAccessResolver courseContentAccessResolver;

    @Override
    @Transactional(readOnly = true)
    public OrganisationCourseContentDTO getContentForCaller(UUID courseUuid) {
        return assemble(courseUuid, courseContentAccessResolver.resolveForCaller(courseUuid));
    }

    @Override
    @Transactional(readOnly = true)
    @Deprecated(since = "2.142.0", forRemoval = true)
    @SuppressWarnings("removal")
    public OrganisationCourseContentDTO getContentForOrganisation(UUID courseUuid, UUID organisationUuid) {
        return assemble(courseUuid, courseContentAccessResolver.resolveForOrganisation(courseUuid, organisationUuid));
    }

    /**
     * One assembly for every viewer, with {@code fullAccess} the only switch.
     * <p>
     * Deliberately not two code paths. Two would eventually disagree, and the day they did the
     * disagreement would be a payload carrying content to somebody entitled to none of it.
     */
    private OrganisationCourseContentDTO assemble(UUID courseUuid, CourseContentAccess access) {
        final boolean fullAccess = access.grantsFullContent();
        final boolean seesDrafts = access.seesDrafts();

        final List<Lesson> lessons = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(courseUuid).stream()
                .filter(lesson -> seesDrafts || isPublished(lesson))
                .toList();

        final List<OrganisationCourseLessonDTO> lessonViews = lessons.stream()
                .map(lesson -> toLessonView(lesson, fullAccess))
                .toList();

        final List<CourseReviewDTO> reviews = courseReviewService.getReviewsForCourse(courseUuid);

        return new OrganisationCourseContentDTO(
                courseUuid,
                access,
                fullAccess,
                lessons.size(),
                averageRating(reviews),
                reviews.size(),
                lessonViews,
                courseProfile(courseUuid));
    }

    /**
     * The course itself, so a public course page needs no second, authenticated request.
     * Null when the row has gone: an empty profile would only turn missing into blank.
     */
    private PublicCourseProfileDTO courseProfile(UUID courseUuid) {
        return courseRepository.findByUuid(courseUuid)
                .map(course -> new PublicCourseProfileDTO(
                        course.getName(),
                        course.getDescription(),
                        course.getObjectives(),
                        course.getPrerequisites(),
                        FileUrlResolver.publicUrl(course.getThumbnailUrl()),
                        FileUrlResolver.publicUrl(course.getBannerUrl()),
                        FileUrlResolver.publicUrl(course.getIntroVideoUrl()),
                        course.getDurationHours(),
                        course.getDurationMinutes(),
                        mappingRepository.findCategoryNamesByCourseUuid(courseUuid),
                        course.getPrice(),
                        course.getClassLimit(),
                        course.getAgeLowerLimit(),
                        course.getAgeUpperLimit(),
                        course.getStatus() == ContentStatus.PUBLISHED,
                        Boolean.TRUE.equals(course.getActive())
                                && Boolean.TRUE.equals(course.getAdminApproved())
                                && (course.getStatus() == ContentStatus.PUBLISHED
                                        || course.getStatus() == ContentStatus.DRAFT),
                        course.getCourseCreatorUuid(),
                        creatorName(course.getCourseCreatorUuid()),
                        trainingRequirements(courseUuid),
                        course.getLastModifiedDate() == null
                                ? null
                                : course.getLastModifiedDate().toString()))
                .orElse(null);
    }

    private String creatorName(UUID courseCreatorUuid) {
        if (courseCreatorUuid == null) {
            return null;
        }
        return courseCreatorLookupService
                .findFullNamesByUuids(List.of(courseCreatorUuid))
                .get(courseCreatorUuid);
    }

    private List<PublicCourseProfileDTO.PublicCourseTrainingRequirement> trainingRequirements(UUID courseUuid) {
        return trainingRequirementRepository.findByCourseUuid(courseUuid).stream()
                .map(requirement -> new PublicCourseProfileDTO.PublicCourseTrainingRequirement(
                        requirement.getName(),
                        requirement.getDescription(),
                        requirement.getQuantity(),
                        requirement.getUnit(),
                        requirement.getRequirementType() == null
                                ? null : requirement.getRequirementType().getValue(),
                        requirement.getProvidedBy() == null
                                ? null : requirement.getProvidedBy().getValue(),
                        requirement.getIsMandatory()))
                .toList();
    }

    /**
     * Without full access the lesson's {@code uuid} is withheld along with its contents.
     * <p>
     * Both, not just the contents: the identifier is the key to
     * {@code GET /courses/{c}/lessons/{l}/content}, so handing it over while withholding the bodies
     * would only move the leak one request away.
     */
    private OrganisationCourseLessonDTO toLessonView(Lesson lesson, boolean fullAccess) {
        final List<LessonContentDTO> content = lessonContentService.getContentByLesson(lesson.getUuid());
        return new OrganisationCourseLessonDTO(
                fullAccess ? lesson.getUuid() : null,
                lesson.getLessonNumber(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getLearningObjectives(),
                content.size(),
                fullAccess ? content : null);
    }

    /**
     * The same publish rule the lesson listing applies, so the two agree about what a viewer
     * without full access is shown.
     * <p>
     * It matters more here than there: this endpoint answers anonymous callers, and a creator's
     * unfinished outline is not catalogue copy. Anyone with full access — the creator included —
     * still sees their drafts, because drafting is what they are here to do.
     */
    private boolean isPublished(Lesson lesson) {
        return lesson.getStatus() == ContentStatus.PUBLISHED && Boolean.TRUE.equals(lesson.getActive());
    }

    private Double averageRating(List<CourseReviewDTO> reviews) {
        final OptionalDouble average = reviews.stream()
                .map(CourseReviewDTO::rating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average();
        return average.isPresent() ? average.getAsDouble() : null;
    }
}
