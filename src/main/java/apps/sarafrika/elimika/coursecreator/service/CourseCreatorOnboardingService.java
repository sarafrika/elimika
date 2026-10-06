package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCategoriesRequest;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorOnboardingStateDTO;

import java.util.UUID;

public interface CourseCreatorOnboardingService {

    CourseCreatorOnboardingStateDTO getCurrentOnboarding();

    CourseCreatorOnboardingStateDTO updateCategories(CourseCreatorCategoriesRequest request);

    CourseCreatorOnboardingStateDTO submitCurrentForVerification();

    CourseCreatorOnboardingStateDTO moderate(UUID courseCreatorUuid, String action, String reason);
}
