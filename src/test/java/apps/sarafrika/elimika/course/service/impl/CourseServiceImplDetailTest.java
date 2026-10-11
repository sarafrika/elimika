package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseCategoryMappingRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseTrainingRequirementService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplDetailTest {

    private static final UUID COURSE_UUID = UUID.randomUUID();
    private static final UUID CREATOR_UUID = UUID.randomUUID();

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseCategoryMappingRepository mappingRepository;
    @Mock
    private CourseTrainingRequirementService courseTrainingRequirementService;
    @Mock
    private CourseCreatorLookupService courseCreatorLookupService;

    @InjectMocks
    private CourseServiceImpl service;

    @BeforeEach
    void stubCourse() {
        Course course = new Course();
        course.setUuid(COURSE_UUID);
        course.setName("Intro to Pottery");
        course.setCourseCreatorUuid(CREATOR_UUID);
        course.setStatus(ContentStatus.PUBLISHED);
        course.setActive(true);
        course.setAdminApproved(true);
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(course));
        when(mappingRepository.findCategoryNamesByCourseUuid(COURSE_UUID)).thenReturn(List.of());
        when(courseTrainingRequirementService.findByCourseUuid(COURSE_UUID)).thenReturn(List.of());
        when(courseCreatorLookupService.findFullNamesByUuids(Set.of(CREATOR_UUID)))
                .thenReturn(Map.of(CREATOR_UUID, "Jane Wanjiku"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCourseByUuidInlinesCreatorName() {
        CourseDTO detail = service.getCourseByUuid(COURSE_UUID);

        assertThat(detail.courseCreatorName()).isEqualTo("Jane Wanjiku");
    }

    @Test
    void getVisibleCourseByUuidInlinesCreatorNameForSignedInCaller() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "learner", null, AuthorityUtils.NO_AUTHORITIES));

        CourseDTO detail = service.getVisibleCourseByUuid(COURSE_UUID);

        assertThat(detail.courseCreatorName()).isEqualTo("Jane Wanjiku");
    }

    @Test
    void getVisibleCourseByUuidKeepsCreatorNameInPublicView() {
        CourseDTO detail = service.getVisibleCourseByUuid(COURSE_UUID);

        assertThat(detail.courseCreatorName()).isEqualTo("Jane Wanjiku");
        assertThat(detail.minimumTrainingFee()).isNull();
    }
}
