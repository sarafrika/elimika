package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.dto.AgeGroupRequest;
import apps.sarafrika.elimika.course.dto.LessonHoursRequest;
import apps.sarafrika.elimika.course.internal.agegroup.AgeGroups;
import apps.sarafrika.elimika.course.internal.agegroup.AgeGroups.AgeGroupScope;
import apps.sarafrika.elimika.course.util.enums.AgeGroupOwnerType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainingApplicationAgeGroupsTest {

    @Mock private AgeGroups ageGroups;
    @InjectMocks private TrainingApplicationAgeGroups policy;

    private final AgeGroupScope scope = new AgeGroupScope(3, 12, List.of(), "this course");
    private final List<AgeGroupRequest> juniors = List.of(new AgeGroupRequest("Juniors", 3, 5,
            List.of(new LessonHoursRequest(UUID.randomUUID(), BigDecimal.ONE))));

    @Test
    @DisplayName("a new instructor submission must carry age groups; an organisation's never does")
    void submissionsRequireGroupsFromInstructors() {
        assertThatThrownBy(() -> policy.requireForSubmission(CourseTrainingApplicantType.INSTRUCTOR, null))
                .hasMessageContaining("At least one age group");
        assertThatCode(() -> policy.requireForSubmission(CourseTrainingApplicantType.ORGANISATION, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("an organisation cannot send age groups, and an instructor cannot send none")
    void applicantRules() {
        assertThatThrownBy(() -> policy.validate(CourseTrainingApplicantType.ORGANISATION, scope, juniors))
                .hasMessageContaining("Only instructor applicants");
        assertThatThrownBy(() -> policy.validate(CourseTrainingApplicantType.INSTRUCTOR, scope, List.of()))
                .hasMessageContaining("At least one age group");
        policy.validate(CourseTrainingApplicantType.INSTRUCTOR, scope, juniors);
        verify(ageGroups).validate(scope, juniors);
    }

    @Test
    @DisplayName("null leaves stored groups alone")
    void nullIsUntouched() {
        policy.validate(CourseTrainingApplicantType.INSTRUCTOR, scope, null);
        verify(ageGroups, never()).validate(any(), any());
    }

    @Test
    @DisplayName("an application's groups are owned by the application, by its type")
    void ownershipFollowsTheApplicationType() {
        UUID application = UUID.randomUUID();
        policy.replace(TrainingApplicationType.PROGRAM, application, juniors);
        verify(ageGroups).replace(AgeGroupOwnerType.PROGRAM_TRAINING_APPLICATION, application, juniors);
    }
}
