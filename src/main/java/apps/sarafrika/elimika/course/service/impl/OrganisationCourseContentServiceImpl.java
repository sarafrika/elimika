package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseReviewDTO;
import apps.sarafrika.elimika.course.dto.LessonContentDTO;
import apps.sarafrika.elimika.course.dto.OrganisationCourseContentDTO;
import apps.sarafrika.elimika.course.dto.OrganisationCourseLessonDTO;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.service.CourseReviewService;
import apps.sarafrika.elimika.course.service.CourseTrainingApplicationService;
import apps.sarafrika.elimika.course.service.LessonContentService;
import apps.sarafrika.elimika.course.service.OrganisationCourseContentService;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganisationCourseContentServiceImpl implements OrganisationCourseContentService {

    private final LessonRepository lessonRepository;
    private final LessonContentService lessonContentService;
    private final CourseReviewService courseReviewService;
    private final CourseTrainingApplicationService courseTrainingApplicationService;

    @Override
    @Transactional(readOnly = true)
    public OrganisationCourseContentDTO getContentForOrganisation(UUID courseUuid, UUID organisationUuid) {
        final boolean approved = courseTrainingApplicationService.hasApprovedApplication(
                courseUuid, CourseTrainingApplicantType.ORGANISATION, organisationUuid);

        final List<Lesson> lessons = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(courseUuid);

        final List<OrganisationCourseLessonDTO> lessonViews = lessons.stream()
                .map(lesson -> toLessonView(lesson, approved))
                .toList();

        final List<CourseReviewDTO> reviews = courseReviewService.getReviewsForCourse(courseUuid);

        return new OrganisationCourseContentDTO(
                courseUuid,
                approved,
                lessons.size(),
                averageRating(reviews),
                reviews.size(),
                lessonViews);
    }

    private OrganisationCourseLessonDTO toLessonView(Lesson lesson, boolean approved) {
        final List<LessonContentDTO> content = lessonContentService.getContentByLesson(lesson.getUuid());
        return new OrganisationCourseLessonDTO(
                approved ? lesson.getUuid() : null,
                lesson.getLessonNumber(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getLearningObjectives(),
                content.size(),
                approved ? content : null);
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
