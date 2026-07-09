package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.RecommendedCourseDTO;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseCategoryMapping;
import apps.sarafrika.elimika.course.repository.CourseCategoryMappingRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRecommendationServiceImplTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseCategoryMappingRepository categoryMappingRepository;
    @Mock
    private CourseTrainingApplicationRepository trainingApplicationRepository;
    @Mock
    private CourseCreatorLookupService courseCreatorLookupService;
    @Mock
    private InstructorLookupService instructorLookupService;

    @InjectMocks
    private CourseRecommendationServiceImpl service;

    private final UUID userUuid = UUID.randomUUID();
    private final UUID creatorUuid = UUID.randomUUID();

    private final UUID pastCourse = UUID.randomUUID();
    private final UUID candidateShared = UUID.randomUUID();
    private final UUID candidateUnrelated = UUID.randomUUID();

    private final UUID categoryShared = UUID.randomUUID();
    private final UUID categoryOther = UUID.randomUUID();
    private final UUID difficulty = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid))
                .thenReturn(Optional.of(creatorUuid));
        when(instructorLookupService.findInstructorUuidByUserUuid(userUuid))
                .thenReturn(Optional.empty());
    }

    private Course course(UUID uuid, String name, UUID difficultyUuid, LocalDateTime created) {
        Course course = new Course();
        course.setUuid(uuid);
        course.setName(name);
        course.setDifficultyUuid(difficultyUuid);
        course.setCreatedDate(created);
        return course;
    }

    private CourseCategoryMapping mapping(UUID categoryUuid) {
        CourseCategoryMapping mapping = new CourseCategoryMapping();
        mapping.setCategoryUuid(categoryUuid);
        return mapping;
    }

    @Test
    void recommendsByCategoryOverlapAndExcludesPastCourses() {
        when(courseRepository.findUuidsByCourseCreatorUuid(creatorUuid)).thenReturn(List.of(pastCourse));

        Course pastEntity = course(pastCourse, "My Past Course", difficulty, LocalDateTime.now().minusDays(10));
        when(courseRepository.findByUuidIn(List.of(pastCourse))).thenReturn(List.of(pastEntity));

        Course shared = course(candidateShared, "Shared Topic Course", difficulty, LocalDateTime.now().minusDays(2));
        Course unrelated = course(candidateUnrelated, "Unrelated Course", null, LocalDateTime.now().minusDays(1));
        when(courseRepository.findByStatus(ContentStatus.PUBLISHED))
                .thenReturn(List.of(pastEntity, shared, unrelated));

        when(categoryMappingRepository.findByCourseUuid(pastCourse)).thenReturn(List.of(mapping(categoryShared)));
        when(categoryMappingRepository.findByCourseUuid(candidateShared)).thenReturn(List.of(mapping(categoryShared)));
        when(categoryMappingRepository.findByCourseUuid(candidateUnrelated)).thenReturn(List.of(mapping(categoryOther)));

        List<RecommendedCourseDTO> result = service.recommendForUser(userUuid, 6);

        assertThat(result).extracting(RecommendedCourseDTO::courseUuid).containsExactly(candidateShared);
        assertThat(result.get(0).reason()).contains("topic");
        assertThat(result.get(0).score()).isGreaterThan(0.0);
    }

    @Test
    void fallsBackToPopularityWhenNoHistory() {
        when(courseRepository.findUuidsByCourseCreatorUuid(creatorUuid)).thenReturn(List.of());
        lenient().when(courseRepository.findByUuidIn(any())).thenReturn(List.of());

        Course newer = course(candidateShared, "Newer Course", null, LocalDateTime.now().minusDays(1));
        Course older = course(candidateUnrelated, "Older Course", null, LocalDateTime.now().minusDays(9));
        when(courseRepository.findByStatus(ContentStatus.PUBLISHED)).thenReturn(List.of(older, newer));

        List<RecommendedCourseDTO> result = service.recommendForUser(userUuid, 6);

        assertThat(result).extracting(RecommendedCourseDTO::courseUuid)
                .containsExactly(candidateShared, candidateUnrelated);
        assertThat(result).allSatisfy(dto -> assertThat(dto.reason()).isEqualTo("Popular right now"));
    }
}
