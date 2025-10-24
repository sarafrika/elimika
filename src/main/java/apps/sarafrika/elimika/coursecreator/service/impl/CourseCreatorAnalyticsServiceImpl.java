package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.shared.spi.analytics.CourseCreatorAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.CourseCreatorAnalyticsSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseCreatorAnalyticsServiceImpl implements CourseCreatorAnalyticsService {

    private final CourseCreatorRepository courseCreatorRepository;

    @Override
    public CourseCreatorAnalyticsSnapshot captureSnapshot() {
        long totalCourseCreators = courseCreatorRepository.count();
        long verifiedCourseCreators = courseCreatorRepository.countByAdminVerified(Boolean.TRUE);
        long pendingCourseCreators = courseCreatorRepository.countByAdminVerified(Boolean.FALSE)
                + courseCreatorRepository.countByAdminVerifiedIsNull();

        return new CourseCreatorAnalyticsSnapshot(
                totalCourseCreators,
                verifiedCourseCreators,
                pendingCourseCreators
        );
    }
}
