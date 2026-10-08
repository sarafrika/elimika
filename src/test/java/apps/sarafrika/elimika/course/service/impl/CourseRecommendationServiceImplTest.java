package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.RecommendedCourseDTO;
import apps.sarafrika.elimika.course.internal.recommend.CandidateCourse;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.Reason;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.ReasonCode;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.ScoredCourse;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.Surface;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CourseRecommendationServiceImplTest {

    @Mock
    private DiscoveryTracker discoveryTracker;

    @InjectMocks
    private CourseRecommendationServiceImpl service;

    @Test
    void publishesStoredThumbnailKeysAsFileUrls() {
        List<RecommendedCourseDTO> response = service.respond(List.of(
                scored("course_thumbnails/x.jpeg"),
                scored("https://cdn.example.com/y.png"),
                scored(null)), Surface.FOR_YOU, null);

        assertThat(response.get(0).thumbnailUrl()).startsWith("/api/v1/files/").endsWith("course_thumbnails/x.jpeg");
        assertThat(response.get(1).thumbnailUrl()).isEqualTo("https://cdn.example.com/y.png");
        assertThat(response.get(2).thumbnailUrl()).isNull();
    }

    private static ScoredCourse scored(String thumbnail) {
        CandidateCourse course = new CandidateCourse(UUID.randomUUID(), "Course", "About", thumbnail,
                LocalDateTime.now(), 1, null, null, null, null, 0, true);
        return new ScoredCourse(course, 1.0, List.of(new Reason(ReasonCode.CATEGORY, "Because", null)), false);
    }
}
